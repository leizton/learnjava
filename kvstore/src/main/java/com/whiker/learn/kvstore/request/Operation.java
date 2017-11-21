package com.whiker.learn.kvstore.request;

/**
 * Created by whiker on 2017/3/26.
 */
public enum Operation {

    // 最大127种
    GET,
    SET,
    SETNX,
    DEL;

    public static Operation of(int ordinal) {
        return values()[ordinal];
    }
}
