package com.whiker.learn.kvstore.response;

/**
 * Created by whiker on 2017/3/28.
 */
public enum RetCode {

    REQUEST_DISCARD((byte) -1),
    REQUEST_INVALID((byte) -2),

    SUCCESS((byte) 0),
    KEY_NOT_FOUND((byte) 1),
    KEY_ALREADY_EXIST((byte) 2);

    private final byte code;

    public byte code() {
        return code;
    }

    public static RetCode ofCode(byte code) {
        for (RetCode retCode : values()) {
            if (retCode.code == code) {
                return retCode;
            }
        }
        return null;
    }

    RetCode(byte code) {
        this.code = code;
    }
}
