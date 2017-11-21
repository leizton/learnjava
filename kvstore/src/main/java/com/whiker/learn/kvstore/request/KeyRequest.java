package com.whiker.learn.kvstore.request;

import com.whiker.learn.kvstore.ex.InvalidRequestException;
import com.whiker.learn.kvstore.util.Util;

import java.nio.ByteBuffer;

/**
 * Created by whiker on 2017/3/26.
 */
public class KeyRequest extends Request {

    public final String key;

    public KeyRequest(Operation operation, int requestId, String key) {
        super(operation, requestId);
        this.key = key;
    }

    /**
     * | size | operation | requestId | key
     * |  4B  |    1B     |    4B     |
     */
    @Override
    public ByteBuffer encode() {
        byte[] bytes = Util.strToBytes(key);
        ByteBuffer buf = ByteBuffer.allocate(9 + bytes.length);
        buf.putInt(5 + bytes.length);
        buf.put((byte) operation.ordinal());
        buf.putInt(requestId);
        buf.put(bytes);
        buf.flip();
        return buf;
    }

    static RequestDecoder decoder() {
        return (operation, requestId, data) -> {
            int keySize = data.remaining();
            if (keySize <= 0) {
                throw new InvalidRequestException("invalid keySize: " + keySize);
            }
            byte[] bytes = new byte[keySize];
            data.get(bytes);
            return new KeyRequest(operation, requestId, Util.bytesToStr(bytes));
        };
    }

    @Override
    public String toString() {
        return "<" + requestId + ", " + operation + ", " + key + ">";
    }
}
