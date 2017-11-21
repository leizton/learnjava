package com.whiker.learn.tianchi.common;

/**
 * 二元组
 *
 * @author whiker@163.com create on 16-10-19.
 */
public class Pair<T1, T2> {

    private T1 v1;
    private T2 v2;

    public static <T1, T2> Pair<T1, T2> of(T1 v1, T2 v2) {
        Pair<T1, T2> p = new Pair<>();
        p.v1 = v1;
        p.v2 = v2;
        return p;
    }

    public T1 getV1() {
        return v1;
    }

    public T2 getV2() {
        return v2;
    }
}
