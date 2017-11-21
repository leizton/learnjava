package com.whiker.learn.kvstore.core;

import com.whiker.learn.kvstore.request.RequestFuture;

/**
 * Created by whiker on 2017/3/29.
 */
public interface KvStoreClient {

    static KvStoreClient newClient(String remoteHost, int remotePort) {
        KvStoreClientImpl client = new KvStoreClientImpl(remoteHost, remotePort);
        try {
            client.start();
            return client;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    RequestFuture<String> get(String key);

    RequestFuture<Void> set(String key, String value);

    RequestFuture<Boolean> setnx(String key, String value);

    RequestFuture<String> del(String key);

    void close();
}
