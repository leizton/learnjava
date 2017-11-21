package com.whiker.learn.kvstore.core;

import com.whiker.learn.kvstore.request.KeyRequest;
import com.whiker.learn.kvstore.request.KeyValueRequest;
import com.whiker.learn.kvstore.request.Request;
import com.whiker.learn.kvstore.response.RetCode;
import com.whiker.learn.kvstore.util.Configuration;
import com.whiker.learn.kvstore.util.Util;
import com.whiker.learn.kvstore.request.Operation;
import com.whiker.learn.kvstore.response.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;

/**
 * Created by whiker on 2017/3/27.
 */
class KvStoreImpl implements Runnable, KvStore {
    private static final Logger logger = LoggerFactory.getLogger(KvStoreImpl.class);

    private volatile boolean isRunning = false;
    private CountDownLatch stopLatch;

    private Map<String, String> store;
    private ArrayBlockingQueue<Response> handleQueue;

    private Thread handleThread;

    void start(ThreadGroup group) {
        isRunning = true;
        stopLatch = new CountDownLatch(1);
        store = new TreeMap<>();
        handleQueue = new ArrayBlockingQueue<>(Configuration.StoreHandleQueueCapacity);
        handleThread = new Thread(group, this, "store");
        handleThread.start();
    }

    void stop() {
        isRunning = false;
        handleThread.interrupt();
        handleThread = null;
        Util.waitLatch(stopLatch);
        stopLatch = null;
        store = null;
        handleQueue = null;
    }

    @Override
    public void accept(Response response) {
        logger.debug("accept {}", response);
        if (!handleQueue.offer(response)) {
            response.setRetcode(RetCode.REQUEST_DISCARD);
            response.send();
        }
    }

    @Override
    public void run() {
        Response curr;
        while (isRunning) {
            try {
                curr = handleQueue.take();
            } catch (InterruptedException e) {
                continue;
            }
            if (curr != null) {
                handle(curr);
            }
        }
        stopLatch.countDown();
    }

    private void handle(Response response) {
        Request request = response.request();
        Handler handler = handlers[request.operation.ordinal()];
        if (handler == null) {
            response.setRetcode(RetCode.REQUEST_INVALID);
            response.setContent("unsupport operation: " + request.operation.name());
        } else {
            handler.handle(store, response);
        }
        logger.debug("handle {}", response);
        response.send();
    }

    private static Handler[] handlers = new Handler[Operation.values().length];

    static {
        handlers[Operation.GET.ordinal()] = new GetHandler();
        handlers[Operation.SET.ordinal()] = new SetHandler();
        handlers[Operation.SETNX.ordinal()] = new SetnxHandler();
        handlers[Operation.DEL.ordinal()] = new DelHandler();
    }

    private interface Handler {
        void handle(Map<String, String> store, Response response);
    }

    private static class GetHandler implements Handler {
        @Override
        public void handle(Map<String, String> store, Response response) {
            KeyRequest request = (KeyRequest) response.request();
            String value = store.get(request.key);
            if (value == null) {
                response.setRetcode(RetCode.KEY_NOT_FOUND);
            } else {
                response.setRetcode(RetCode.SUCCESS);
                response.setContent(value);
            }
        }
    }

    private static class SetHandler implements Handler {
        @Override
        public void handle(Map<String, String> store, Response response) {
            KeyValueRequest request = (KeyValueRequest) response.request();
            store.put(request.key, request.value);
            response.setRetcode(RetCode.SUCCESS);
        }
    }

    private static class SetnxHandler implements Handler {
        @Override
        public void handle(Map<String, String> store, Response response) {
            KeyValueRequest request = (KeyValueRequest) response.request();
            if (store.containsKey(request.key)) {
                response.setRetcode(RetCode.KEY_ALREADY_EXIST);
            } else {
                store.put(request.key, request.value);
                response.setRetcode(RetCode.SUCCESS);
            }
        }
    }

    private static class DelHandler implements Handler {
        @Override
        public void handle(Map<String, String> store, Response response) {
            KeyRequest request = (KeyRequest) response.request();
            String value = store.remove(request.key);
            if (value == null) {
                response.setRetcode(RetCode.KEY_NOT_FOUND);
            } else {
                response.setRetcode(RetCode.SUCCESS);
                response.setContent(value);
            }
        }
    }
}
