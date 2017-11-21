package com.whiker.learn.tianchi.wifiap;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import com.google.common.io.LineProcessor;
import com.whiker.learn.tianchi.common.FileUtil;
import com.whiker.learn.tianchi.common.TimeUtil;
import com.whiker.learn.tianchi.common.Pair;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

/**
 * 对原始wifi接入点表的转换
 * 结果文件的每行是: accessPointName,startTime,passengerNum_1,passengerNum_2,...
 * passengerNum_1是startTime时这个接入点的连接人数
 * passengerNum_i==-1表示这个时间点的数据缺失
 *
 * @author whiker@163.com create on 16-10-18.
 */
public class WifiApFileTransform implements LineProcessor {

    static final String SEPERATOR = ",";

    private static final String SRC_FILEPATH = FileUtil.getRootPath() + "/src/WIFI_AP_Passenger_Records_chusai_2ndround.csv";
    static final String DST_FILEPATH = FileUtil.getRootPath() + "/data/src/wifi_ap.txt";

    public static void main(String[] args) throws Exception {
        WifiApFileTransform transformer = new WifiApFileTransform();
        Files.readLines(new File(SRC_FILEPATH), Charset.forName("UTF-8"), transformer);
    }

    private Map<String, Integer> wifiAps = Maps.newHashMapWithExpectedSize(800);

    private List<List<Pair<Integer, Integer>>> results = Lists.newArrayListWithCapacity(800);

    private int apCount = 0;

    private boolean isFirstLine = true;
    private int readCount = 0;

    @Override
    public boolean processLine(String line) throws IOException {
        if (isFirstLine) {
            isFirstLine = false;
            return true;
        }
        String[] data = line.split(",");
        if (data.length != 3) {
            return true;
        }

        int apIndex = getIndexOfAp(data[0]);
        int passengerNum = parsePassengerNum(data[1]);
        int time = TimeUtil.getTimeByWifiApFormat(data[2]);
        if (passengerNum >= 0 && time >= 0) {
            record(apIndex, passengerNum, time);
        }
        if (++readCount % 100000 == 0) {
            System.out.println(readCount);
        }
        return true;
    }

    private int getIndexOfAp(String apName) {
        Integer apIndex = wifiAps.get(apName);
        if (apIndex == null) {
            apIndex = apCount++;
            wifiAps.put(apName, apIndex);
        }
        return apIndex;
    }

    private int parsePassengerNum(String s) {
        if (Strings.isNullOrEmpty(s)) {
            return -1;
        }
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private void record(int apIndex, int passengerNum, int time) {
        if (results.size() <= apIndex) {
            results.add(Lists.newArrayListWithCapacity(30000));
        }
        List<Pair<Integer, Integer>> ret = results.get(apIndex);
        ret.add(Pair.of(passengerNum, time));
    }

    @Override
    public Object getResult() {
        sortResults();
        saveResults();
        return null;
    }

    private void sortResults() {
        results.parallelStream().forEach(lst -> lst.sort(
                (p1, p2) -> p1.getV2() - p2.getV2()  // 按time从小到大排序
        ));
    }

    private void saveResults() {
        String[] aps = new String[wifiAps.size()];
        int i = 0;
        for (Map.Entry<String, Integer> e : wifiAps.entrySet()) {
            aps[i++] = e.getKey();
        }

        try (BufferedWriter writer = FileUtil.getWriter(DST_FILEPATH)) {
            i = 1;
            for (String apName : aps) {
                writer.write(getResultString(apName));
                writer.newLine();
                System.out.println(i++);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String getResultString(String apName) {
        int apIndex = wifiAps.get(apName);
        List<Pair<Integer, Integer>> ret = results.get(apIndex);
        int prevTime = ret.get(0).getV2();

        StringBuilder str = new StringBuilder(ret.size() * 3);
        str.append(apName).append(SEPERATOR).append(prevTime);
        str.append(SEPERATOR).append(ret.get(0).getV1());

        for (int i = 1; i < ret.size(); i++) {
            int time = ret.get(i).getV2();
            for (prevTime++; prevTime < time; prevTime++) {
                str.append(SEPERATOR).append(-1);
            }
            str.append(SEPERATOR).append(ret.get(i).getV1());
        }
        return str.toString();
    }
}
