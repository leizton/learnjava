package com.whiker.learn.common;

import java.nio.charset.Charset;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

public class Util {

    public static final Charset UTF8 = Charset.forName("utf-8");

    public static String toString(Exception ex) {
        return ex == null ? "null" : ex.getClass().getName() + ": " + ex.getMessage();
    }

    public static void awaitIgnoreInterrupted(CountDownLatch latch) {
        while (true) {
            try {
                latch.await();
                break;
            } catch (InterruptedException e) {
                // ignore
            }
        }
    }

    public static String generateId() {
        Optional<String> ip = NetUtil.localIp();
        if (ip.isPresent()) {
            return ip.get() + "%" + UUID.randomUUID().toString();
        }
        return UUID.randomUUID().toString();
    }
}
