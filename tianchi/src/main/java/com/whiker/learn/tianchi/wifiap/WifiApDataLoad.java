package com.whiker.learn.tianchi.wifiap;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.google.common.io.LineProcessor;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.List;

/**
 * @author whiker@163.com create on 16-10-19.
 */
public class WifiApDataLoad implements LineProcessor<List<WifiAp>> {

    private static List<WifiAp> WIFI_APS;

    static {
        WIFI_APS = load();
    }

    public static List<WifiAp> get() {
        return WIFI_APS;
    }

    private static List<WifiAp> load() {
        try {
            return Files.readLines(new File(WifiApFileTransform.DST_FILEPATH),
                    Charset.forName("UTF-8"), new WifiApDataLoad());
        } catch (IOException e) {
            throw new RuntimeException("load wifiAps error", e);
        }
    }

    private List<WifiAp> wifiAps = Lists.newArrayListWithExpectedSize(800);

    private int wifiApsCount = 0;

    private Splitter lineSplitter = Splitter.on(WifiApFileTransform.SEPERATOR).trimResults();

    @Override
    public boolean processLine(String line) {
        wifiAps.add(parse(line, wifiApsCount++));
        return true;
    }

    private WifiAp parse(String line, int apId) {
        Iterator<String> it = lineSplitter.split(line).iterator();
        WifiAp ap = new WifiAp();
        ap.setId(apId);
        parseNameAndStartTime(it, ap);
        parsetPassengerNums(it, ap);
        return ap;
    }

    private void parseNameAndStartTime(Iterator<String> it, WifiAp ap) {
        if (it == null || !it.hasNext()) {
            throw new ParseException("lost name, wifiApId:" + ap.getId());
        }
        ap.setName(it.next());

        if (!it.hasNext()) {
            throw new ParseException("lost startTime, wifiApId:" + ap.getId());
        }
        try {
            ap.setStartTime(Integer.parseInt(it.next()));
        } catch (NumberFormatException e) {
            throw new ParseException("parse startTime error, wifiApId:" + ap.getId(), e);
        }
    }

    private void parsetPassengerNums(Iterator<String> it, WifiAp ap) {
        List<Integer> nums = Lists.newArrayListWithExpectedSize(20000);
        while (it.hasNext()) {
            try {
                nums.add(Integer.parseInt(it.next()));
            } catch (NumberFormatException e) {
                throw new ParseException("parse passenger num error, wifiApId:"
                        + ap.getId() + ", position:" + nums.size(), e);
            }
        }
        int[] arr = new int[nums.size()];
        for (int i = nums.size() - 1; i >= 0; i--) {
            arr[i] = nums.get(i);
        }
        ap.setData(arr);
    }

    @Override
    public List<WifiAp> getResult() {
        return wifiAps;
    }

    private static class ParseException extends RuntimeException {
        private ParseException(String message) {
            super(message);
        }

        private ParseException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
