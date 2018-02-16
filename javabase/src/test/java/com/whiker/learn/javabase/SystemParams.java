package com.whiker.learn.javabase;

import org.junit.Test;

public class SystemParams {

    public static String getUserHome() {
        return System.getProperty("user.home");
    }

    @Test
    public void testUserHomeDir() {
        System.out.println(getUserHome());
    }
}
