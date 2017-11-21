package com.whiker.learn.kvstore.channel;

import com.whiker.learn.kvstore.core.KvStore;
import com.whiker.learn.kvstore.ex.InvalidRequestException;
import com.whiker.learn.kvstore.request.Request;
import com.whiker.learn.kvstore.response.Response;
import com.whiker.learn.kvstore.response.RetCode;
import com.whiker.learn.kvstore.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

/**
 * Created by whiker on 2017/3/26.
 */
public class PacketChannel {
    private static final Logger logger = LoggerFactory.getLogger(PacketChannel.class);

    private final SocketChannel socketChannel;
    private final SelectionKey selectionKey;
    private Packet packet;
    private ByteBuffer send;

    private final KvStore store;

    public PacketChannel(SocketChannel socketChannel, SelectionKey selectionKey, KvStore store) {
        this.socketChannel = socketChannel;
        this.selectionKey = selectionKey;
        this.packet = new Packet();
        this.store = store;
    }

    public String remoteHost() {
        try {
            return socketChannel.getRemoteAddress().toString();
        } catch (IOException e) {
            logger.error("get remote host error", e);
            return "";
        }
    }

    public void read() throws Exception {
        boolean isCompleted = packet.read(socketChannel);
        if (isCompleted) {
            removeSelectionKeyOp(SelectionKey.OP_READ);
            try {
                Request request = Request.decode(packet.takeData());
                store.accept(new Response(this, request));
            } catch (InvalidRequestException e) {
                Response response = new Response(this, null);
                response.setRetcode(RetCode.REQUEST_INVALID);
                response.setContent(e.getMessage());
                send(response.encode());
            }
        }
    }

    public void send(ByteBuffer send) {
        this.send = send;
        addSelectionKeyOp(SelectionKey.OP_WRITE);
    }

    public void write() throws IOException {
        if (send == null) {
            return;
        }
        if (send.hasRemaining()) {
            socketChannel.write(send);
        }
        if (!send.hasRemaining()) {
            send = null;
            removeSelectionKeyOp(SelectionKey.OP_WRITE);
            addSelectionKeyOp(SelectionKey.OP_READ);
        }
    }

    public void close() {
        Util.closeSocketChannel(socketChannel);
    }

    private void removeSelectionKeyOp(int op) {
        selectionKey.interestOps(selectionKey.interestOps() & (~op));
    }

    private void addSelectionKeyOp(int op) {
        selectionKey.interestOps(selectionKey.interestOps() | op);
    }
}
