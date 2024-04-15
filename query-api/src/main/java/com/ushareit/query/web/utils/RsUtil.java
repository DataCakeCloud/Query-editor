package com.ushareit.query.web.utils;

import com.alibaba.fastjson.JSONObject;
import com.ushareit.query.constant.ProbeConstant;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class RsUtil {
    private static Pattern digestPattern = Pattern.compile("^[-\\+]?[\\d]*$");

    private static String repeat_column(ArrayList<String> column, String element) {
        String result = "";
        if (column.size() == 0) {
            return result;
        }
        int count = 0;
        ArrayList<String> num = new ArrayList<>();
        for(int i=0; i<column.size(); i++){
            if (element.equals(column.get(i))) {
                count += 1;
            }
            if (count == 1 && column.get(i).startsWith(element)) {
                String[] content = column.get(i).split(element);
                if (content.length == 0){
                    result = "1";
                }
                if (content.length == 2 && digestPattern.matcher(content[1]).matches()) {
                    num.add(content[1]);
                }
            }
        }
        if (num.size() == 0) {
            if (count > 0) {
                result = String.format("%d", count);
            }
            return result;
        }
        if (count == 1) {
            int n = Integer.parseInt(num.get(num.size()-1));
            result = String.format("%d", n+1);
        }
        return result;
    }

    public static JSONObject getColumns(ResultSet rs) throws SQLException {
        String result = "";
        ArrayList<String> column = new ArrayList<String>();
        ArrayList<Map<String, String>> type = new ArrayList<>();
        Map<String, String> repeat_columns = new HashMap<>();
        JSONObject columns = new JSONObject();
        for(int i=0; i<rs.getMetaData().getColumnCount(); i++) {
            result = repeat_column(column, stripTableName(rs.getMetaData().getColumnName(i+1)));
            if (result.equals("")) {
                column.add(stripTableName(rs.getMetaData().getColumnName(i+1)));
            } else {
                column.add(rs.getMetaData().getColumnName(i+1) + result);
                repeat_columns.put(stripTableName(rs.getMetaData().getColumnName(i+1)) + result, stripTableName(rs.getMetaData().getColumnName(i+1)));
            }
            Map<String, String> column_type = new HashMap<>();
            column_type.put(column.get(i), ProbeConstant.columnTypeMap.get(rs.getMetaData().getColumnType(i+1)));
            type.add(column_type);
        }
        columns.put("type", type);
        columns.put("column", column);
        columns.put("repeat", repeat_columns);

        return columns;
    }

    public static String stripTableName(String colName) {
        int idx = colName.indexOf('.');
        if (-1 == idx) {
            return colName;
        }
        return colName.substring(idx + 1);
    }
}
