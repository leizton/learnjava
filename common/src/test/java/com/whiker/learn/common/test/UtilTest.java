package com.whiker.learn.common.test;

import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

/**
 * 2019-04-01
 */
public class UtilTest {

    @Test
    public void testSwitch() {
        int[] status = new int[]{200, 404, 500};
        for (var statu : status) {
            var ret = switch (statu) {
                case 200 -> "ok";
                default -> "fail";
            };
            System.out.println(ret);
        }
    }

    @Test
    public void test() {
        Assert.assertEquals("UTF-8", StandardCharsets.UTF_8.name());
    }
}
