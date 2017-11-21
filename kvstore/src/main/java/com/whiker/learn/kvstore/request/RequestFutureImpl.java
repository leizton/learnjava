package com.whiker.learn.kvstore.request;

import com.whiker.learn.kvstore.response.ResponseParser;

import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * @author yiqun.fan create on 17-3-29.
 */
public class RequestFutureImpl<T> implements RequestFuture {
    private static final byte UNRECEIVED = 0;
    private static final byte RECEIVED = 1;
    private static final byte DONE = 2;

    public final int requestId;
    public final ByteBuffer request;
    private final ResponseParser<T> responseParser;
    private ByteBuffer response;
    private T result;
    private ExecutionException parseException;
    private byte state = UNRECEIVED;

    public RequestFutureImpl(int requestId, ByteBuffer request, ResponseParser<T> responseParser) {
        this.requestId = requestId;
        this.request = request;
        this.responseParser = responseParser;
    }

    // 解析过程交给用户线程
    public synchronized void received(ByteBuffer response) {
        this.response = response;
        state = RECEIVED;
        this.notifyAll();
    }

    @Override
    public int requestId() {
        return requestId;
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
        synchronized (this) {
            while (state == UNRECEIVED) {
                this.wait();
            }
            parseAndDone();
        }
        return result;
    }

    @Override
    public T get(long timeoutMilliseconds) throws InterruptedException, ExecutionException, TimeoutException {
        if (timeoutMilliseconds <= 0) {
            throw new IllegalArgumentException("timeoutMilliseconds <= 0: " + timeoutMilliseconds);
        }

        long mark = System.currentTimeMillis();
        synchronized (this) {
            if (state == UNRECEIVED) {
                timeoutMilliseconds = timeoutMilliseconds - (System.currentTimeMillis() - mark);
                if (timeoutMilliseconds > 0) {
                    this.wait(timeoutMilliseconds);
                }
            }
            if (state == UNRECEIVED) {
                throw new TimeoutException();
            }
            parseAndDone();
        }
        return result;
    }

    private void parseAndDone() throws ExecutionException {
        if (!isDone()) {
            try {
                result = responseParser.parse(response);
            } catch (Exception e) {
                if (e instanceof ExecutionException) {
                    parseException = (ExecutionException) e;
                } else {
                    parseException = new ExecutionException(e);
                }
                throw parseException;
            } finally {
                response = null;
                state = DONE;
            }
        } else if (parseException != null) {
            throw parseException;
        }
    }

    @Override
    public boolean isDone() {
        return state == DONE;
    }
}
