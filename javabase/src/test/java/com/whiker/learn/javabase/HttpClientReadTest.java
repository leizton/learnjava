package com.whiker.learn.javabase;

import com.sun.xml.internal.messaging.saaj.util.ByteInputStream;
import com.whiker.learn.common.thread.NamedThreadFactory;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.stream.ChunkedStream;
import io.netty.handler.stream.ChunkedWriteHandler;
import org.asynchttpclient.AsyncCompletionHandler;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.UUID;

import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class HttpClientReadTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpClientReadTest.class);

    private static final int PORT = 10010;
    private static final String NO_CHUNKED_URI = "/nochunked";
    private static final String CHUNKED_URI = "/chunked";

    public static void main(String[] args) throws Exception {
        startServer();
        testClient(NO_CHUNKED_URI);
        testClient(CHUNKED_URI);
    }

    /**
     * 如果不加ChunkedWriteHandler, ctx.writeAndFlush()不支持ChunkedStream, client收不到数据
     * 调用ctx.writeAndFlush(new ChunkedStream(inp)).sync()时抛一个warn异常
     * 不调用sync()不会有提醒
     */
    private static void startServer() throws Exception {
        EventLoopGroup group = new NioEventLoopGroup(1, new NamedThreadFactory("netty-server"));
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(group)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new HttpServerCodec())
                                .addLast(new HttpObjectAggregator(1 << 20))
                                .addLast(new ChunkedWriteHandler())
                                .addLast(new HttpHandler());
                    }
                });
        Channel channel = bootstrap.bind(PORT).sync().channel();
        channel.closeFuture().addListener(future -> group.shutdownGracefully());
    }

    private static class HttpHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
        private static final int LINE_NUM = 5;
        private static final int LINE_LENGTH = 31;
        private static final int LINE_SIZE = LINE_LENGTH + 1;

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
            HttpResponse response = new DefaultHttpResponse(HTTP_1_1, OK);
            if (request.uri().equals(NO_CHUNKED_URI)) {
                response.headers().set(HttpHeaderNames.CONTENT_LENGTH, LINE_NUM * LINE_SIZE);
                ctx.write(response);
                for (int i = 0; i < LINE_NUM; ++i) {
                    ctx.writeAndFlush(random(ctx.alloc()));
                    Thread.sleep(1000);
                }
            } else {
                response.headers().set(HttpHeaderNames.TRANSFER_ENCODING, HttpHeaderValues.CHUNKED);
                ctx.write(response);
                for (int i = 0; i < LINE_NUM; ++i) {
                    byte[] bs = random();
                    ByteInputStream inp = new ByteInputStream(bs, bs.length);
                    ctx.writeAndFlush(new ChunkedStream(inp, bs.length));
                    Thread.sleep(1000);
                }
            }
            ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT)
                    .addListener(ChannelFutureListener.CLOSE);
        }

        private static ByteBuf random(ByteBufAllocator alloc) {
            byte[] bs = random();
            ByteBuf buf = alloc.buffer(bs.length, bs.length);
            buf.writeBytes(bs);
            return buf;
        }

        private static byte[] random() {
            String line = UUID.randomUUID().toString().substring(0, LINE_LENGTH);
            return (line + "\n").getBytes();
        }
    }

    /**
     * nochunked和chunked情况下，用BufferedReader::readLine()都是非阻塞的
     * 说明client是获取全部的body，再转成Stream
     */
    private static void testClient(final String uri) throws Exception {
        AsyncHttpClient client = new DefaultAsyncHttpClient();
        client.prepareGet("http://127.0.0.1:" + PORT + uri).execute(new AsyncCompletionHandler<Object>() {
            @Override
            public Object onCompleted(Response response) throws Exception {
                long time = System.currentTimeMillis();
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(response.getResponseBodyAsStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    LOGGER.info("receive {}", line);
                }
                LOGGER.info("{} read use {}s", uri, (System.currentTimeMillis() - time) / 1000);
                return null;
            }
        }).get();
    }
}
