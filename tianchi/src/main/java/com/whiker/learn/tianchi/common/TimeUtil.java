package com.whiker.learn.tianchi.common;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * @author whiker@163.com create on 16-10-18.
 */
public class TimeUtil {

    // 原始wifi接入点表的日期格式
    private static final DateTimeFormatter WIFI_AP_FORMAT = DateTimeFormat.forPattern("yyyy-MM-dd-HH-mm-ss");

    // 时间戳的0参考时间
    private static final DateTime START_TIME = DateTime.parse("2016-09-10-00-00-00", WIFI_AP_FORMAT);

    private static final long START_ABSENT_TIME = START_TIME.getMillis();

    public static int getTimeByWifiApFormat(String s) {
        try {
            DateTime time = DateTime.parse(s, WIFI_AP_FORMAT);
            long diffInMinute = (time.getMillis() - START_ABSENT_TIME) / 60000;
            if (diffInMinute < 0) {
                return -1;
            }
            if (diffInMinute > Integer.MAX_VALUE) {
                return Integer.MAX_VALUE;
            }
            return (int) diffInMinute;
        } catch (Exception e) {
            return -1;
        }
    }
}
