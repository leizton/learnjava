package com.whiker.learn.kvstore.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.Channel;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.concurrent.CountDownLatch;

/**
 * Created by whiker on 2017/3/26.
 */
public class Util {
    private static final Logger logger = LoggerFactory.getLogger(Util.class);

    private static final Charset UTF8 = Charset.forName("UTF-8");

    public static void waitLatch(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            logger.error("waitLatch be interrupted", e);
        }
    }

    public static void closeServerSocketChannel(ServerSocketChannel ssc) {
        if (ssc != null) {
            closeChannelAndSocket(ssc, ssc.socket(), "ServerSocketChannel");
        }
    }

    public static void closeSocketChannel(SocketChannel sc) {
        if (sc != null) {
            closeChannelAndSocket(sc, sc.socket(), "SocketChannel");
        }
    }

    private static void closeChannelAndSocket(Channel channel, Closeable socket, String channelType) {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                logger.error("close {}'s socket error", channelType, e);
            }
        }
        try {
            channel.close();
        } catch (IOException e) {
            logger.error("close {} error", channelType, e);
        }
    }

    public static void closeSelector(Selector selector) {
        if (selector != null) {
            try {
                selector.close();
            } catch (IOException e) {
                logger.error("close selector error", e);
            }
        }
    }

    public static byte[] strToBytes(String s) {
        if (s == null || s.length() == 0) {
            return new byte[0];
        }
        return s.getBytes(UTF8);
    }

    public static String bytesToStr(byte[] bs) {
        return bytesToStr(bs, 0, bs.length);
    }

    public static String bytesToStr(byte[] bs, int offset, int length) {
        if (bs == null || bs.length == 0) {
            return "";
        }
        return new String(bs, offset, length, UTF8);
    }
}
