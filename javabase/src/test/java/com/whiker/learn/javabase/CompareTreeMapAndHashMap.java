package com.whiker.learn.javabase;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Function;

public class CompareTreeMapAndHashMap {

    /**
     *
     * ratio = 10
     215	334	1.5534883720930233
     179	457	2.553072625698324
     372	723	1.9435483870967742
     374	583	1.5588235294117647
     310	799	2.57741935483871
     *
     * ratio = 100
     1012	2480	2.450592885375494
     1156	9290	8.036332179930795
     1713	17095	9.979568009340339
     2512	24687	9.827627388535031
     3304	33403	10.109866828087167
     *
     */
    public static void main(String[] args) {
        final int testNum = 5;
        final int ratio = 100;
        final int keySize = 32;
        final int runNum = 100000;

        for (int iTest = 1; iTest <= testNum; ++iTest) {
            int entryNum = iTest * ratio;
            String[] keys = new String[entryNum];
            String[] values = new String[entryNum];
            for (int i = 0; i < entryNum; ++i) {
                keys[i] = UUID.randomUUID().toString().substring(0, keySize);
                values[i] = UUID.randomUUID().toString().substring(0, keySize);
            }
            long t1 = test(entryNum, runNum, keys, values, v -> new HashMap<>());
            long t2 = test(entryNum, runNum, keys, values, v -> new TreeMap<>());
            System.out.println(t1 + "\t" + t2 + "\t" + (t2 / (double) t1));
        }
    }

    private static long test(int entryNum, int runNum, String[] keys, String[] values, Function<Void, Map<String, String>> mapFactory) {
        long time = System.currentTimeMillis();
        for (int iRun = 0; iRun < runNum; ++iRun) {
            Map<String, String> map = mapFactory.apply(null);
            for (int i = 0; i < entryNum; ++i) {
                map.put(keys[i], values[i]);
            }
            for (int i = 0; i < entryNum; ++i) {
                if (!values[i].equals(map.get(keys[i]))) {
                    throw new RuntimeException();
                }
            }
            for (int i = 0; i < entryNum; ++i) {
                if (!values[i].equals(map.get(keys[i]))) {
                    throw new RuntimeException();
                }
            }
            for (int i = 0; i < entryNum; ++i) {
                map.put(keys[i], values[i]);
            }
            for (int i = 0; i < entryNum; ++i) {
                if (!values[i].equals(map.get(keys[i]))) {
                    throw new RuntimeException();
                }
            }
            for (int i = 0; i < entryNum; ++i) {
                if (!values[i].equals(map.get(keys[i]))) {
                    throw new RuntimeException();
                }
            }
        }
        return System.currentTimeMillis() - time;
    }
}
