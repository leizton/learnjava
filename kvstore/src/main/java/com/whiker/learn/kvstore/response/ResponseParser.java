package com.whiker.learn.kvstore.response;

import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;

/**
 * Created by whiker on 2017/3/29.
 */
public interface ResponseParser<T> {

    T parse(ByteBuffer response) throws ExecutionException;
}
