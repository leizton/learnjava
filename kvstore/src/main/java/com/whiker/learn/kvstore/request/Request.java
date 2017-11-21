package com.whiker.learn.kvstore.request;

import com.whiker.learn.kvstore.ex.InvalidRequestException;

import java.nio.ByteBuffer;

/**
 * Created by whiker on 2017/3/26.
 */
public abstract class Request {

    interface RequestDecoder {
        Request decode(Operation operation, int requestId, ByteBuffer data) throws InvalidRequestException;
    }

    private static class RequestDecoderHolder {
        private static final RequestDecoder[] decoders = new RequestDecoder[Operation.values().length];

        private static void register(Operation op, RequestDecoder decoder) {
            decoders[op.ordinal()] = decoder;
        }

        static {
            RequestDecoder keyRequestDecoder = KeyRequest.decoder();
            register(Operation.GET, keyRequestDecoder);
            register(Operation.DEL, keyRequestDecoder);

            RequestDecoder keyValueRequestDecoder = KeyValueRequest.decoder();
            register(Operation.SET, keyValueRequestDecoder);
            register(Operation.SETNX, keyValueRequestDecoder);
        }
    }

    public static Request decode(ByteBuffer data) throws InvalidRequestException {
        if (!data.hasRemaining()) {
            throw new InvalidRequestException("empty request");
        }
        int operation = (int) data.get();
        if (operation < 0 || operation > Operation.values().length) {
            throw new InvalidRequestException("unsupport operation: " + operation);
        }
        if (data.remaining() < 4) {
            throw new InvalidRequestException("lost requestId");
        }
        int requestId = data.getInt();
        return RequestDecoderHolder.decoders[operation].decode(Operation.values()[operation], requestId, data);
    }

    public final Operation operation;
    public final int requestId;

    Request(Operation operation, int requestId) {
        this.operation = operation;
        this.requestId = requestId;
    }

    public abstract ByteBuffer encode();
}
