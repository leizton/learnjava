package com.whiker.learn.kvstore.channel;

import com.whiker.learn.kvstore.util.Configuration;
import com.whiker.learn.kvstore.ex.InvalidPacketException;

import java.io.EOFException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

/**
 * Created by whiker on 2017/3/26.
 */
public class Packet {
    private ByteBuffer size = ByteBuffer.allocate(4);
    private ByteBuffer data = null;

    public ByteBuffer takeData() {
        ByteBuffer ret = data;
        clear();
        return ret;
    }

    public boolean read(ReadableByteChannel channel) throws Exception {
        if (size.hasRemaining()) {
            // read size
            if (channel.read(size) < 0) {
                size.rewind();
                throw new EOFException();
            }
            if (size.hasRemaining()) {
                return false;
            }

            // prepare to read data
            size.rewind();
            int dataSize = size.getInt();
            if (dataSize < 5 || dataSize > Configuration.PacketMaxSize) {
                size.rewind();
                throw new InvalidPacketException("invalid data size: " + dataSize);
            }
            data = ByteBuffer.allocate(dataSize);
        }

        // read data
        try {
            if (channel.read(data) < 0) {
                clear();
                throw new EOFException();
            }
            if (!data.hasRemaining()) {
                size.rewind();
                data.rewind();
                return true;
            }
        } catch (Exception e) {
            clear();
            throw e;
        }
        return false;
    }

    private void clear() {
        size.rewind();
        data = null;
    }
}
