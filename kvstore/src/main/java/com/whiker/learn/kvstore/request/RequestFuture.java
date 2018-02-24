package com.whiker.learn.kvstore.request;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * @author leizton create on 17-3-29.
 */
public interface RequestFuture<T> {

    int requestId();

    T get() throws InterruptedException, ExecutionException;

    T get(long timeoutMilliseconds) throws InterruptedException, ExecutionException, TimeoutException;

    boolean isDone();
}
