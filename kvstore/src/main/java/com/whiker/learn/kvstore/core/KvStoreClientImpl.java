package com.whiker.learn.kvstore.core;

import com.google.common.base.Strings;
import com.whiker.learn.kvstore.channel.Packet;
import com.whiker.learn.kvstore.request.*;
import com.whiker.learn.kvstore.response.ResponseParser;
import com.whiker.learn.kvstore.response.ResponseParserHolder;
import com.whiker.learn.kvstore.util.Configuration;
import com.whiker.learn.kvstore.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Created by whiker on 2017/3/29.
 */
class KvStoreClientImpl implements KvStoreClient, Runnable {
    private static final Logger logger = LoggerFactory.getLogger(KvStoreClientImpl.class);

    private final String remoteHost;
    private final int remotePort;

    private SocketChannel socketChannel;
    private Selector selector;

    private volatile boolean isRunning = false;
    private volatile CountDownLatch startLatch;
    private CountDownLatch stopLatch;

    private AtomicInteger requestId;
    private Map<Integer, RequestFutureImpl> requestMap;
    private ArrayBlockingQueue<RequestFutureImpl> requestQueue;

    private RequestFutureImpl currentSend;
    private Packet responsePacket;

    KvStoreClientImpl(String remoteHost, int remotePort) {
        this.remoteHost = remoteHost;
        this.remotePort = remotePort;
    }

    void start() throws IOException, InterruptedException {
        socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(false);
        socketChannel.socket().setTcpNoDelay(true);
        socketChannel.socket().setKeepAlive(true);
        if (!socketChannel.connect(new InetSocketAddress(remoteHost, remotePort))) {
            while (!socketChannel.finishConnect()) {
                logger.debug("connecting");
            }
        }

        selector = Selector.open();
        socketChannel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);

        requestId = new AtomicInteger(0);
        requestMap = new TreeMap<>();
        requestQueue = new ArrayBlockingQueue<>(Configuration.ClientRequestQueueCapacity);
        currentSend = null;
        responsePacket = new Packet();

        isRunning = true;
        startLatch = new CountDownLatch(1);
        stopLatch = new CountDownLatch(1);
        new Thread(new ThreadGroup(Configuration.ThreadGroupName), this, "client").start();
        startLatch.await();
        startLatch = null;
        logger.info("kvstore client available");
    }

    @Override
    public void close() {
        isRunning = false;
        Util.waitLatch(stopLatch);
        stopLatch = null;
        Util.closeSocketChannel(socketChannel);
        Util.closeSelector(selector);
        destory();
    }

    private void destory() {
        requestId = null;
        requestMap = null;
        requestQueue = null;
        currentSend = null;
        responsePacket = null;
    }

    @Override
    public void run() {
        startLatch.countDown();
        boolean isValid = true;
        while (isRunning && isValid) {
            try {
                if (selector.select(500) <= 0) {
                    continue;
                }
            } catch (IOException e) {
                logger.error("select error", e);
                break;
            }

            Iterator<SelectionKey> it = selector.selectedKeys().iterator();
            while (it.hasNext() && isRunning) {
                SelectionKey key = it.next();
                it.remove();

                if (!key.isValid()) {
                    key.cancel();
                    logger.error("selection key invalid");
                    isValid = false;
                    break;
                }

                try {
                    if (key.isWritable()) {
                        sendRequest();
                    }
                    if (key.isReadable()) {
                        receiveResponse();
                    }
                } catch (Exception e) {
                    logger.error("error", e);
                    key.cancel();
                    isValid = false;
                    break;
                }
            }
        }
        stopLatch.countDown();
        if (isRunning) {
            close();
        }
    }

    private void addNewRequest(RequestFutureImpl future) {
        requestMap.put(future.requestId, future);
        try {
            requestQueue.put(future);
            logger.debug("added {}", future.requestId);
        } catch (InterruptedException e) {
            requestMap.remove(future.requestId);
            throw new RuntimeException(e);  // maybe a bad influence to User-Thread
        }
    }

    private void sendRequest() throws IOException, InterruptedException {
        if (currentSend == null) {
            currentSend = requestQueue.poll();
            if (currentSend == null) {
                return;
            }
        }
        socketChannel.write(currentSend.request);
        if (!currentSend.request.hasRemaining()) {
            logger.debug("sended {}", currentSend.requestId);
            currentSend = null;
        }
    }

    private void receiveResponse() throws Exception {
        boolean isCompleted = responsePacket.read(socketChannel);
        if (isCompleted) {
            ByteBuffer data = responsePacket.takeData();
            if (data.remaining() < 4) {
                logger.error("lost request id");
                return;
            }
            int requestId = data.getInt();
            logger.debug("received {}", requestId);
            RequestFutureImpl future = requestMap.remove(requestId);
            if (future == null) {
                logger.error("lost request, id: {}", requestId);
            } else {
                future.received(data);
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public RequestFuture<String> get(String key) {
        checkArgument(!Strings.isNullOrEmpty(key), "empty key");
        Request request = new KeyRequest(Operation.GET, requestId.getAndIncrement(), key);
        return newFuture(request);
    }

    @Override
    @SuppressWarnings("unchecked")
    public RequestFuture<Void> set(String key, String value) {
        checkArgument(!Strings.isNullOrEmpty(key), "empty key");
        Request request = new KeyValueRequest(Operation.SET, requestId.getAndIncrement(), key, Strings.nullToEmpty(value));
        return newFuture(request);
    }

    @Override
    @SuppressWarnings("unchecked")
    public RequestFuture<Boolean> setnx(String key, String value) {
        checkArgument(!Strings.isNullOrEmpty(key), "empty key");
        Request request = new KeyValueRequest(Operation.SETNX, requestId.getAndIncrement(), key, Strings.nullToEmpty(value));
        return newFuture(request);
    }

    @Override
    @SuppressWarnings("unchecked")
    public RequestFuture<String> del(String key) {
        checkArgument(!Strings.isNullOrEmpty(key), "empty key");
        Request request = new KeyRequest(Operation.DEL, requestId.getAndIncrement(), key);
        return newFuture(request);
    }

    @SuppressWarnings("unchecked")
    private RequestFuture newFuture(Request request) {
        logger.debug("new {}", request);
        ResponseParser responseParser = ResponseParserHolder.getParser(request.operation);
        if (responseParser == null) {
            throw new RuntimeException("unsupport operation: " + request.operation);
        }

        RequestFutureImpl future = new RequestFutureImpl<>(request.requestId, request.encode(), responseParser);
        addNewRequest(future);
        return future;
    }
}
