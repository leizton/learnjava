package com.whiker.learn.kvstore.test;

import com.whiker.learn.kvstore.request.RequestFuture;

/**
 * Created by whiker on 2017/3/31.
 */
class TestUtil {

    static void test(RequestFuture future, Object expect) {
        Object result;
        try {
            result = future.get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        boolean pass = (expect == null && result == null) || (expect != null && expect.equals(result));
        if (pass) {
            System.out.println("Pass " + future.requestId() + " > " + result);
        } else {
            System.out.println("Error " + future.requestId() + " > expect: " + expect + ", result: " + result);
        }
    }
}
