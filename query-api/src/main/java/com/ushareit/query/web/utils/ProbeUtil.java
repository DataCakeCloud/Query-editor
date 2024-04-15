package com.ushareit.query.web.utils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import tech.tablesaw.api.ColumnType;
import tech.tablesaw.api.Table;
import tech.tablesaw.columns.Column;
import tech.tablesaw.io.csv.CsvReadOptions;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static tech.tablesaw.api.ColumnType.*;
import static tech.tablesaw.api.QuerySupport.and;

public class ProbeUtil {
    private static DecimalFormat format = new DecimalFormat("#0.00");

    private static ArrayList<Double> handleSumAverage(Column<?> columns, Integer total) {
        ArrayList<Double> result = new ArrayList<>();
        double sum = 0.0;
        for (Object row: columns) {
            if ((Integer) row != -2147483648) {
                double num = Double.parseDouble(String.valueOf(row));
                sum += num;
            }
        }
        result.add(sum);
        result.add(sum/total);
        return result;
    }

    public static Map<String, Object> probeCSV(String file, JSONArray type, JSONObject sample) throws IOException {
        Map<String, Object> probeData = new HashMap<>();
        ArrayList<Map<String, Object>> result = new ArrayList<>();

        Table table = Table.read().csv(file);
        int total = table.rowCount();
        probeData.put("total", total);

        for (int i=0; i<type.size(); i++) {
            JSONObject map = type.getJSONObject(i);
            String columnName = (String) map.keySet().toArray()[0];
            String columnType = (String) map.get(columnName);

            Map<String, Object> column = new HashMap<>();
            Map<String, Object> statistics = new HashMap<>();
            switch (columnType) {
                case "string":
                case "date":
                    // top2: null与空字符串均被当作空字符串，显示为null
                    Table percents = table.xTabPercents(columnName).sortOn("-Percents");
                    Object top1_name = percents.column("Category").get(0);
                    double top1_ratio = Double.parseDouble(format.format((Double) percents.column("Percents").get(0) * 100));
                    if (top1_name.equals("")) {
                        top1_name = "null";
                    }
                    if (top1_ratio == Double.parseDouble(format.format(((double) 1/total * 100)))) {
                        statistics.put("unique values", total);
                        int nullCount = table.column(columnName).countMissing();
                        if (nullCount > 0) {
                            statistics.put("null", nullCount);
                        }
                        statistics.put("integer", "true");
                        break;
                    }
                    String top1_vlaue = String.valueOf(top1_name);
                    String tableSample = table.column(columnName).getString(1);
                    String resultSample = "";
                    if (sample != null) {
                        resultSample = sample.getString(columnName);
                        if (resultSample != null) {
                            if (columnType.equals("string") && tableSample.length() > resultSample.length() && tableSample.contains("-")) {
                                top1_vlaue = top1_vlaue.replace("-", "");
                            }
                        }
                    }
                    statistics.put(top1_vlaue, top1_ratio);

                    double top2_ratio = 0;
                    if (top1_ratio < 100) {
                        Object top2_name = percents.column("Category").get(1);
                        top2_ratio = Double.parseDouble(format.format((Double) percents.column("Percents").get(1) * 100));
                        if (top2_name.equals("")) {
                            top2_name = "null";
                        }
                        String top2_vlaue = String.valueOf(top2_name);
                        if (sample != null) {
                            if (columnType.equals("string") && tableSample.length() > resultSample.length() && tableSample.contains("-")) {
                                top2_vlaue = top2_vlaue.replace("-", "");
                            }
                        }
                        statistics.put(top2_vlaue, top2_ratio);
                    }

                    if (top1_ratio+top2_ratio < 100) {
                        statistics.put("otherField", Double.parseDouble(format.format(100 - top1_ratio - top2_ratio)));
                    }
                    double nullString = (double) table.column(columnName).countMissing() / total * 100;
                    double nullStringProportion = Double.parseDouble(String.format("%.2f", nullString));
                    if (nullStringProportion >= 0.01) {
                        statistics.put("null", nullStringProportion);
                    }

                    break;
                case "integer":
                case "double":
                case "float":
                    //todo: 统计出sum、averge、min、max聚合值， 以及直方图
                    Table summary = table.column(columnName).summary();
                    if (summary.rowCount()==4 && summary.column("Measure").getString(1).equals("Unique")
                            && summary.column("Value").getString(1).equals("1")) {
                        statistics.put("null", table.column(columnName).countMissing());
                        break;
                    }

                    int nullCount = table.column(columnName).countMissing();
                    if (nullCount > 0) {
                        statistics.put("null", nullCount);
                    }
                    // 计算sum与average时，不能有null与空值
                    String sum = summary.column("Value").getString(1);
                    String average = summary.column("Value").getString(2);
                    String min = summary.column("Value").getString(3);
                    String max = summary.column("Value").getString(4);

                    ArrayList<Map<String, Integer>> histogram = new ArrayList<>();
                    if (min.equals(max)) {
                        Map<String, Integer> unique = new HashMap<>();
                        unique.put(String.format("%s-%s", min, max), total);
                        histogram.add(unique);
                        statistics.put("histogram", histogram);
                        break;
                    }

                    if (sum.equals("")) {
                        ArrayList<Double> sumAverage = handleSumAverage(table.column(columnName), total);
                        statistics.put("sum", format.format(sumAverage.get(0)));
                        statistics.put("average", format.format(sumAverage.get(1)));
                    } else {
                        statistics.put("sum", sum);
                        statistics.put("average", format.format(new BigDecimal(average)));
                    }
                    statistics.put("min", min);
                    statistics.put("max", max);

                    double start = Double.parseDouble(min);
                    double end = Double.parseDouble(max);
                    double step = (end - start) / 10;

                    for (int j=0; j<10; j++) {
                        double finalStart = start;
                        if (j == 9) {
                            step += 1.0;
                        }
                        double midpoint = start + step;

                        String mark = String.format("%s-%s", format.format(start), format.format(midpoint));
                        if (j == 9) {
                            mark = String.format("%s-%s", format.format(start), format.format(end));
                        }

                        int cnt = table.where(and(
                                t -> t.numberColumn(columnName).isGreaterThanOrEqualTo(finalStart),
                                t -> t.numberColumn(columnName).isLessThan(midpoint)
                        )).rowCount();
                        start = midpoint;

                        Map<String, Integer> interval = new HashMap<>();
                        interval.put(mark, cnt);
                        histogram.add(interval);
                    }
                    statistics.put("histogram", histogram);

                    break;
                case "boolean":
                    // 饼图占比
                    int trueCount = table.where(t -> t.booleanColumn(columnName).isTrue()).rowCount();
                    double trueProportion = Double.parseDouble(String.format("%.2f", (double) trueCount / total * 100));
                    statistics.put("true", trueProportion);

                    int falseCount = table.where(t -> t.booleanColumn(columnName).isFalse()).rowCount();
                    double falseProportion = Double.parseDouble(String.format("%.2f", (double) falseCount / total * 100));
                    statistics.put("false", falseProportion);
                    if (trueProportion + falseProportion < 100.0) {
                        double nullBool = (double) table.column(columnName).countMissing() / total * 100;
                        double nullBoolProportion = Double.parseDouble(String.format("%.2f", nullBool));
                        if (nullBoolProportion >= 0.01) {
                            statistics.put("null", nullBoolProportion);
                        }
                    }
                    break;
            }

            column.put("type", columnType);
            column.put("volume", columnName);
            column.put("statistics", statistics);

            result.add(column);
        }
        probeData.put("result", result);
        return probeData;
    }
}
