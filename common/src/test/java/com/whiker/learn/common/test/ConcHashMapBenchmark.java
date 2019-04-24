package com.whiker.learn.common.test;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

/**
 * 2019-04-24
 */
public class ConcHashMapBenchmark {

    private static String[] testKeys(int num) {
        var keys = new String[num];
        for (int i = 0; i < num; i++) {
            keys[i] = String.valueOf(i) + String.valueOf(num - i);
        }
        return keys;
    }

    private static void hashMapBenchmark(String[] keys, final int runNum, final int readNum) {
        var data = new ConcurrentHashMap<String, String>();
        var time = System.currentTimeMillis();

        for (int i = 0; i < runNum; i++) {
            for (var e : keys) {
                data.put(e, "abc123");
            }
            for (int j = 0; j < readNum; j++) {
                for (var e : keys) {
                    if (!data.containsKey(e)) {
                        System.out.println("error");
                        return;
                    }
                }
            }
            data.clear();
            System.gc();
        }

        time = System.currentTimeMillis() - time;
        System.out.println(time);
    }

    private static void concHashMapBenchmark(String[] keys, final int runNum,
                                             final int readNum, final int threadNum) throws Exception {
        final var data = new ConcurrentHashMap<String, String>();
        final int taskNum = keys.length / threadNum;
        var time = System.currentTimeMillis();

        var latch1 = new CountDownLatch(threadNum);
        for (int id = 0; id < threadNum; id++) {
            int keyBegin = id * taskNum;
            int keyEnd = Math.min((id + 1) * taskNum, keys.length);
            new Thread(() -> {
                for (int i = 0; i < runNum; i++) {
                    for (int j = keyBegin; j < keyEnd; j++) {
                        data.put(keys[j], "abc123");
                    }
                }
                latch1.countDown();
            }).start();
        }
        latch1.await();

        var latch2 = new CountDownLatch(threadNum);
        for (int id = 0; id < threadNum; id++) {
            new Thread(() -> {
                for (int i = 0; i < runNum; i++) {
                    for (int j = 0; j < readNum; j++) {
                        for (var e : keys) {
                            if (!data.containsKey(e)) {
                                System.out.println("error");
                                latch2.countDown();
                                return;
                            }
                        }
                    }
                }
                latch2.countDown();
            }).start();
        }
        latch2.await();

        time = System.currentTimeMillis() - time;
        System.out.println(time);
    }

    public static void main(String[] args) throws Exception {
        final int mapSize = 40000;
        var keys = testKeys(mapSize);

        hashMapBenchmark(keys, 200, 10);  // 3140ms
        hashMapBenchmark(keys, 200, 10);  // 2863ms
        concHashMapBenchmark(keys, 200, 10, 4);  // 1461
    }
}
