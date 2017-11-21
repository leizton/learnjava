package com.whiker.learn.tianchi.v1;

import com.google.common.collect.Lists;
import com.whiker.learn.tianchi.result.ForecastRet;
import com.whiker.learn.tianchi.result.ForecastRetSave;
import com.whiker.learn.tianchi.wifiap.WifiAp;
import com.whiker.learn.tianchi.wifiap.WifiApDataLoad;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author whiker@163.com create on 16-10-19.
 */
public class SolutionV1 {

    private static final int DAY_NUM = 10;
    private static final int ONE_DAY_SPAN = 24 * 60;  // 24个小时，每小时60分钟

    public static void main(String[] args) {
        ForecastRetSave.save(new SolutionV1().solve(), "/data/ret/v1");
    }

    private List<ForecastRet> solve() {
        return WifiApDataLoad.get().parallelStream()
                .map(this::solveOne)
                .sorted((ret1, ret2) -> ret1.getAp().getId() - ret2.getAp().getId())
                .collect(Collectors.toList());
    }

    private ForecastRet solveOne(WifiAp ap) {
        final int startTime = ForecastRet.START_TIME - DAY_NUM * ONE_DAY_SPAN;
        final int endTime = ForecastRet.START_TIME - ONE_DAY_SPAN;

        List<ForecastRet> rets = Lists.newArrayListWithExpectedSize(DAY_NUM);
        for (int time = startTime; time < endTime; time += ONE_DAY_SPAN) {
            ForecastRet oneDayRet = meanOfOneDay(ap, time, time + ForecastRet.STAT_SPAN, ForecastRet.MEAN_SPAN);
            rets.add(oneDayRet);
        }

        return meanOfDays(rets);
    }

    private ForecastRet meanOfOneDay(WifiAp ap, final int startTime, final int endTime, final int meanSpan) {
        ForecastRet ret = new ForecastRet(ap);
        for (int time = startTime; time < endTime; ) {
            int mean = 0, meanCnt = 0;
            for (int i = 0; i < meanSpan; i++) {
                int num = ap.getData(time++);
                if (num >= 0) {
                    mean += num;
                    meanCnt++;
                }
            }
            mean = meanCnt > 0 ? (mean / meanCnt) : -1;
            ret.addRet(mean);
        }
        return ret;
    }

    private ForecastRet meanOfDays(List<ForecastRet> rets) {
        WifiAp ap = rets.get(0).getAp();
        if (!rets.stream().allMatch(ret -> ret.getRetNum() == ForecastRet.RET_NUM)) {
            throw new RuntimeException("ret num != ForecastRet.RET_NUM, wifiApName:"
                    + ap.getName() + ", wifiApId:" + ap.getId());
        }
        final int pointNum = rets.size();
        int[][] matrix = new int[pointNum][];
        for (int i = 0; i < rets.size(); i++) {
            matrix[i] = rets.get(i).getRets();
        }
        ForecastRet result = new ForecastRet(ap);
        for (int i = 0; i < ForecastRet.RET_NUM; i++) {
            int mean = 0, count = 0;
            for (int j = 0; j < pointNum; j++) {
                if (matrix[j][i] > 0) {
                    count++;
                    mean += matrix[j][i];
                }
            }
            mean = count > 0 ? (mean / count) : 0;
            result.addRet(mean);
        }
        return result;
    }
}
