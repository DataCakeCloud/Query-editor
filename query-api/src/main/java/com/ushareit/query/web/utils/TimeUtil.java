package com.ushareit.query.web.utils;

import java.math.RoundingMode;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class TimeUtil {
    public static String getNow() {
        LocalDateTime now = LocalDateTime.now();
        return now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));
    }

    public static float getTimeFloat(long value) {
        DecimalFormat df = new DecimalFormat("0.00");
        df.setRoundingMode(RoundingMode.HALF_UP);
        return  (float)Double.parseDouble(df.format(value * 1.0));
    }

    public static float getTimeDiff(String start, String end) throws ParseException {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        Date startDate = df.parse(start);
        Date endDate = df.parse(end);
        return getTimeFloat(endDate.getTime() - startDate.getTime()) / 1000;
    }
}
