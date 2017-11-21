package com.whiker.learn.kvstore.core;

import com.whiker.learn.kvstore.util.Configuration;
import com.whiker.learn.kvstore.util.Util;
import com.whiker.learn.kvstore.channel.PacketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.EOFException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.concurrent.CountDownLatch;

/**
 * Created by whiker on 2017/3/22.
 */
public class KvStoreServer {
    private static final Logger logger = LoggerFactory.getLogger(KvStoreServer.class);

    private boolean hasStarted = true;

    private final int port;
    private final int processorNum;

    private Selector selector;
    private ServerSocketChannel serverSocketChannel;

    private Acceptor acceptor;
    private Processor[] processors;

    private volatile CountDownLatch startLatch;

    private KvStoreImpl store;

    public KvStoreServer(int port, int processorNum) {
        this.port = port;
        this.processorNum = processorNum;
        this.processors = new Processor[processorNum];
    }

    public synchronized void start() throws IOException, InterruptedException {
        if (!hasStarted) {
            logger.info("kvstore server already running");
            return;  // 最多有一个在运行
        }
        logger.info("kvstore server starting");

        ThreadGroup group = new ThreadGroup(Configuration.ThreadGroupName);
        store = new KvStoreImpl();
        store.start(group);

        try {
            selector = Selector.open();
            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.bind(new InetSocketAddress(port));
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

            startLatch = new CountDownLatch(1 + processorNum);
            for (int i = 0; i < processorNum; i++) {
                processors[i] = new Processor();
                processors[i].start();
                new Thread(group, processors[i], "processor-" + i).start();
            }
            this.acceptor = new Acceptor();
            new Thread(group, acceptor, "acceptor").start();
            hasStarted = true;
            startLatch.await();
            startLatch = null;
            logger.info("kvstore server started");
        } catch (IOException e) {
            hasStarted = false;
            throw e;
        }
    }

    public void stop() {
        if (store != null) {
            store.stop();
            store = null;
        }
        if (acceptor != null) {
            acceptor.stop();
            acceptor = null;
        }
        hasStarted = false;
    }

    private class Acceptor implements Runnable {
        private volatile boolean isRunning = true;
        private CountDownLatch stopLatch = new CountDownLatch(1);
        private int processSelect = 0;

        @Override
        public void run() {
            startLatch.countDown();
            boolean isKeyValid = true;
            while (isRunning && isKeyValid) {
                try {
                    if (selector.select(100) <= 0) {
                        continue;
                    }
                } catch (IOException e) {
                    logger.error("Acceptor select error", e);
                    break;
                }

                Iterator<SelectionKey> it = selector.selectedKeys().iterator();
                while (it.hasNext() && isRunning) {
                    SelectionKey key = it.next();
                    it.remove();

                    if (!key.isValid()) {
                        logger.error("key is invalid");
                        key.cancel();
                        isKeyValid = false;
                        break;
                    }

                    if (key.isAcceptable()) {
                        SocketChannel socketChannel = null;
                        try {
                            socketChannel = serverSocketChannel.accept();
                        } catch (IOException e) {
                            logger.error("serverSocketChannel.accept() error", e);
                        }
                        if (socketChannel == null || socketChannel.socket() == null) {
                            Util.closeSocketChannel(socketChannel);
                        } else {
                            accept(socketChannel);
                        }
                    } else {
                        logger.error("unknown SelectionKey state");
                    }
                }
            }
            stopLatch.countDown();
            if (isRunning) {
                stop();
            }
        }

        private void accept(SocketChannel socketChannel) {
            try {
                socketChannel.configureBlocking(false);
                socketChannel.socket().setTcpNoDelay(true);
                socketChannel.socket().setKeepAlive(true);
                processors[(processSelect++) % processorNum].accept(socketChannel);
                logger.info("accept {}", socketChannel.getRemoteAddress().toString());
            } catch (IOException e) {
                logger.error("Acceptor accept error", e);
                Util.closeSocketChannel(socketChannel);
            }
        }

        private void stop() {
            if (isRunning) {
                isRunning = false;
                Util.waitLatch(stopLatch);
                stopLatch = null;
                Util.closeServerSocketChannel(serverSocketChannel);
                Util.closeSelector(selector);
                for (int i = 0; i < processorNum; i++) {
                    if (processors[i] != null) {
                        processors[i].stop();
                        processors[i] = null;
                    }
                }
            }
        }
    }

    private class Processor implements Runnable {
        private volatile boolean isRunning = false;
        private CountDownLatch stopLatch;
        private Selector selector;

        private void start() throws IOException {
            selector = Selector.open();
            stopLatch = new CountDownLatch(1);
            isRunning = true;
        }

        private void accept(SocketChannel socketChannel) throws ClosedChannelException {
            SelectionKey selectionKey = socketChannel.register(selector, SelectionKey.OP_READ);
            PacketChannel packetChannel = new PacketChannel(socketChannel, selectionKey, store);
            selectionKey.attach(packetChannel);
        }

        @Override
        public void run() {
            startLatch.countDown();
            while (isRunning) {
                try {
                    if (selector.select(500) <= 0) {
                        continue;
                    }
                } catch (Exception e) {
                    logger.error("select error", e);
                    break;
                }

                Iterator<SelectionKey> it = selector.selectedKeys().iterator();
                while (it.hasNext() && isRunning) {
                    SelectionKey key = it.next();
                    PacketChannel channel = (PacketChannel) key.attachment();
                    if (channel == null) {
                        logger.warn("key attachment is null");
                        continue;
                    }
                    it.remove();

                    if (!key.isValid()) {
                        logger.warn("key is invalid, {}", channel.remoteHost());
                        key.channel();
                        channel.close();
                        continue;
                    }

                    try {
                        if (key.isReadable()) {
                            channel.read();
                        }
                        if (key.isWritable()) {
                            channel.write();
                        }
                    } catch (Exception e) {
                        if (e instanceof EOFException) {
                            logger.warn("remote closed: {}", channel.remoteHost());
                        } else {
                            logger.warn("remoteHost: {}", channel.remoteHost(), e);
                        }
                        key.cancel();
                        channel.close();
                    }
                }
            }
            stopLatch.countDown();
            if (isRunning) {
                acceptor.stop();
            }
        }

        private void stop() {
            if (isRunning) {
                isRunning = false;
                Util.waitLatch(stopLatch);
                stopLatch = null;
                Util.closeSelector(selector);
            }
        }
    }
}
