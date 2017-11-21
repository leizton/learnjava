package com.whiker.learn.kvstore.response;

import com.whiker.learn.kvstore.util.Util;
import com.whiker.learn.kvstore.request.Operation;

import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;

/**
 * Created by whiker on 2017/3/29.
 */
public class ResponseParserHolder {

    private static final ResponseParser[] parsers = new ResponseParser[Operation.values().length];

    private static void register(Operation operation, ResponseParser parser) {
        parsers[operation.ordinal()] = parser;
    }

    public static ResponseParser getParser(Operation operation) {
        return parsers[operation.ordinal()];
    }

    static {
        ValueParser valueParser = new ValueParser();
        register(Operation.GET, valueParser);
        register(Operation.DEL, valueParser);

        SetParser setParser = new SetParser();
        register(Operation.SET, setParser);

        SetnxParser setnxParser = new SetnxParser();
        register(Operation.SETNX, setnxParser);
    }

    private static class ValueParser implements ResponseParser<String> {

        @Override
        public String parse(ByteBuffer response) throws ExecutionException {
            RetCode retCode = parseRetCode(response);
            if (retCode == RetCode.SUCCESS) {
                if (response.hasRemaining()) {
                    byte[] value = new byte[response.remaining()];
                    response.get(value);
                    return Util.bytesToStr(value);
                } else {
                    return "";
                }
            }
            return null;
        }
    }

    private static class SetParser implements ResponseParser<Void> {

        @Override
        public Void parse(ByteBuffer response) throws ExecutionException {
            parseRetCode(response);
            return null;
        }
    }

    private static class SetnxParser implements ResponseParser<Boolean> {

        @Override
        public Boolean parse(ByteBuffer response) throws ExecutionException {
            RetCode retCode = parseRetCode(response);
            return retCode == RetCode.SUCCESS;
        }
    }

    private static RetCode parseRetCode(ByteBuffer response) throws ExecutionException {
        if (!response.hasRemaining()) {
            throw new ExecutionException("lost data", null);
        }
        byte code = response.get();
        RetCode retCode = RetCode.ofCode(code);
        if (retCode == null) {
            throw new ExecutionException("unknown retCode: " + (int) code, null);
        }
        if (retCode == RetCode.REQUEST_DISCARD) {
            throw new ExecutionException("request discarded", null);
        }
        if (retCode == RetCode.REQUEST_INVALID) {
            String message;
            if (response.hasRemaining()) {
                byte[] bs = new byte[response.remaining()];
                response.get(bs);
                message = Util.bytesToStr(bs);
            } else {
                message = "";
            }
            throw new ExecutionException(message, null);
        }
        return retCode;
    }
}
