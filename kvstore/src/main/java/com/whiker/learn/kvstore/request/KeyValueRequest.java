package com.whiker.learn.kvstore.request;

import com.whiker.learn.kvstore.util.Util;
import com.whiker.learn.kvstore.ex.InvalidRequestException;

import java.nio.ByteBuffer;

/**
 * Created by whiker on 2017/3/26.
 */
public class KeyValueRequest extends Request {

    public final String key;
    public final String value;

    public KeyValueRequest(Operation operation, int requestId, String key, String value) {
        super(operation, requestId);
        this.key = key;
        this.value = value;
    }

    /**
     * | size | operation | requestId | keySize |      key      | value
     * |  4B  |    1B     |    4B     |   4B    | keySize bytes |
     */
    @Override
    public ByteBuffer encode() {
        byte[] keyBytes = Util.strToBytes(key);
        byte[] valueBytes = Util.strToBytes(value);
        ByteBuffer buf = ByteBuffer.allocate(13 + keyBytes.length + valueBytes.length);
        buf.putInt(9 + keyBytes.length + valueBytes.length);
        buf.put((byte) operation.ordinal());
        buf.putInt(requestId);
        buf.putInt(keyBytes.length);
        buf.put(keyBytes);
        buf.put(valueBytes);
        buf.flip();
        return buf;
    }

    static RequestDecoder decoder() {
        return (operation, requestId, data) -> {
            if (data.remaining() < 4) {
                throw new InvalidRequestException("lost keySize");
            }
            int keySize = data.getInt();
            if (keySize <= 0) {
                throw new InvalidRequestException("invalid keySize: " + keySize);
            }
            int valueSize = data.remaining() - keySize;
            byte[] bytes = new byte[Math.max(keySize, valueSize)];

            data.get(bytes, 0, keySize);
            String key = Util.bytesToStr(bytes, 0, keySize);
            data.get(bytes, 0, valueSize);
            String value = Util.bytesToStr(bytes, 0, valueSize);
            return new KeyValueRequest(operation, requestId, key, value);
        };
    }

    @Override
    public String toString() {
        return "<" + requestId + ", " + operation + ", " + key + ", " + value + ">";
    }
}
