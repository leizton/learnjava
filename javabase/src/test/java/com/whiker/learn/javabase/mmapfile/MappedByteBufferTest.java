package com.whiker.learn.javabase.mmapfile;

import com.whiker.learn.common.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.UUID;

public class MappedByteBufferTest {
    private static final Logger log = LoggerFactory.getLogger(MappedByteBufferTest.class);

    public static void main(String[] args) throws Exception {
        final String filePath = TestConfig.ROOT_PATH + "/MappedByteBufferTest.dat";
        final int reverseFileSize = 1024;
        final String data = UUID.randomUUID().toString();

        File file = new File(filePath);
        FileChannel fileChannel = new RandomAccessFile(file, "rw").getChannel();
        MappedByteBuffer mapBuf = fileChannel.map(FileChannel.MapMode.READ_WRITE, 0, reverseFileSize);
        log.info("mapBuf init, hasArray={}. {}", mapBuf.hasArray(), mapBuf);

        mapBuf.put(data.getBytes(Util.UTF8));
        log.info("write {}. {}", data.length(), mapBuf);

        ByteBuffer read = mapBuf.asReadOnlyBuffer();
        log.info("readOnly init. {}", read);
        read.flip();
        log.info("readOnly flip. {}", read);
        byte[] bytes = new byte[read.limit()];
        read.get(bytes);
        log.info("readOnly after get. {}", read);
        log.info("read check: {}", data.equals(new String(bytes, Util.UTF8)));

        // 保存的文件大小是reverseFileSize
        // https://docs.oracle.com/javase/7/docs/api/java/nio/channels/FileChannel.html#force(boolean)
        // FileChannel的force()不完全包括MappedByteBuffer的修改
        fileChannel.force(false);
        fileChannel.close();
    }
}
