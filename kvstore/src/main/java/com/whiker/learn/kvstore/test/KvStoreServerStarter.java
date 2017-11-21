package com.whiker.learn.kvstore.test;

import com.whiker.learn.kvstore.core.KvStoreServer;
import com.whiker.learn.kvstore.util.Configuration;

/**
 * Created by whiker on 2017/3/30.
 */
public class KvStoreServerStarter {

    public static void main(String[] args) throws Exception {
        KvStoreServer server = new KvStoreServer(Configuration.DefaultPort, Runtime.getRuntime().availableProcessors());
        server.start();
        System.in.read();
        server.stop();
    }
}
