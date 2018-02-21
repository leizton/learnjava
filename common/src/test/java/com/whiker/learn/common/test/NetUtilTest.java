package com.whiker.learn.common.test;

import com.whiker.learn.common.NetUtil;
import org.junit.Test;

/**
 * @author yiqun.fan create on 17-3-9.
 */
public class NetUtilTest {

    @Test
    public void test() {
        System.out.println(NetUtil.localIp());
    }
}
