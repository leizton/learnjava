package com.whiker.learn.tianchi.result;

import com.whiker.learn.tianchi.common.TimeUtil;
import com.whiker.learn.tianchi.wifiap.WifiAp;

/**
 * 预测结果
 *
 * @author whiker@163.com create on 16-10-19.
 */
public class ForecastRet {

    static final String DAY_STR = "2016-09-25";

    static final int START_HOUR = 15;

    static final int RET_NUM_OF_ONE_HOUR = 6;

    // 预测的开始时间
    public static final int START_TIME = TimeUtil.getTimeByWifiApFormat(DAY_STR + "-" + START_HOUR + "-00-00");

    // 预测的结束时间
    private static final int END_TIME = TimeUtil.getTimeByWifiApFormat("2016-09-25-18-00-00");

    // 总统计的时间间隔
    public static final int STAT_SPAN = END_TIME - START_TIME;

    // 求平均的时间间隔，10分钟
    public static final int MEAN_SPAN = 60 / RET_NUM_OF_ONE_HOUR;

    public static final int RET_NUM = STAT_SPAN / MEAN_SPAN;

    private WifiAp ap;
    private int[] rets;
    private int count = 0;

    public ForecastRet(WifiAp ap) {
        this.ap = ap;
        this.rets = new int[RET_NUM];
    }

    public WifiAp getAp() {
        return ap;
    }

    public int[] getRets() {
        return rets;
    }

    public void addRet(int ret) {
        rets[count++] = ret;
    }

    public int getRetNum() {
        return count;
    }
}
