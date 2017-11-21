package com.whiker.learn.tianchi.result;

import com.whiker.learn.tianchi.common.FileUtil;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;

/**
 * 保存预测结果
 *
 * @author whiker@163.com create on 16-10-19.
 */
public class ForecastRetSave {

    private static final String FILENAME = "/airport_gz_passenger_predict.csv";

    public static void save(List<ForecastRet> rets, String dirpath) {
        String filepath = FileUtil.getRootPath() + dirpath + FILENAME;
        try (BufferedWriter writer = FileUtil.getWriter(filepath)) {
            writeTitle(writer);
            writeResultTable(rets, writer);
        } catch (IOException e) {
            throw new RuntimeException("save error", e);
        }
    }

    private static void writeTitle(BufferedWriter writer) throws IOException {
        writer.write("passengerCount,WIFIAPTag,slice10min");
        writer.newLine();
    }

    private static void writeResultTable(List<ForecastRet> rets, BufferedWriter writer) throws IOException {
        for (ForecastRet ret : rets) {
            String wifiApName = ret.getAp().getName();
            int hour = ForecastRet.START_HOUR;
            int minuteCount = 0;
            for (int passengerNum : ret.getRets()) {
                writer.write(String.format("%d,%s,%s-%d-%d", passengerNum, wifiApName,
                        ForecastRet.DAY_STR, hour, minuteCount));
                writer.newLine();
                if (++minuteCount == ForecastRet.RET_NUM_OF_ONE_HOUR) {
                    minuteCount = 0;
                    hour++;
                }
            }
        }
    }
}



















