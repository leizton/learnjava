package com.whiker.learn.kvstore.response;

import com.whiker.learn.kvstore.request.Request;
import com.whiker.learn.kvstore.util.Util;
import com.whiker.learn.kvstore.channel.PacketChannel;

import java.nio.ByteBuffer;

/**
 * Created by whiker on 2017/3/28.
 */
public class Response {

    private final PacketChannel channel;
    private final Request request;

    private byte retcode = (byte) -1;
    private byte[] content;

    public Response(PacketChannel channel, Request request) {
        this.channel = channel;
        this.request = request;
    }

    public Request request() {
        return request;
    }

    public void setRetcode(RetCode retcode) {
        this.retcode = retcode.code();
    }

    public void setContent(String content) {
        this.content = Util.strToBytes(content);
    }

    public void send() {
        channel.send(encode());
    }

    /**
     * | size | requestId | retcode | content
     * |  4B  |    4B     |   1B    |
     */
    public ByteBuffer encode() {
        int contentSize = content != null ? content.length : 0;
        ByteBuffer buf = ByteBuffer.allocate(9 + contentSize);
        buf.putInt(5 + contentSize);
        buf.putInt(request.requestId);
        buf.put(retcode);
        if (contentSize > 0) {
            buf.put(content);
        }
        buf.flip();
        return buf;
    }

    @Override
    public String toString() {
        return "<" + request.requestId + ", " + (int) retcode +
                ((content != null && content.length > 0) ? ", " + Util.bytesToStr(content) : "") + ">";
    }
}
