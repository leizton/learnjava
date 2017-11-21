package com.whiker.learn.kvstore.test;

import com.whiker.learn.kvstore.core.KvStoreClient;
import com.whiker.learn.kvstore.request.RequestFuture;
import com.whiker.learn.kvstore.util.Configuration;

/**
 * Created by whiker on 2017/3/30.
 */
public class KvStoreClientBaseTest {

    public static void main(String[] args) {
        KvStoreClient client = KvStoreClient.newClient("127.0.0.1", Configuration.DefaultPort);

        RequestFuture<String> future = client.get("hello");
        TestUtil.test(future, null);
        TestUtil.test(future, null);  // 可重复取值

        RequestFuture<Void> future1 = client.set("hello", "world");
        TestUtil.test(future1, null);

        future = client.get("hello");
        TestUtil.test(future, "world");

        RequestFuture<Boolean> future2 = client.setnx("hello", "hi");
        TestUtil.test(future2, false);

        future = client.del("hello");
        TestUtil.test(future, "world");

        future2 = client.setnx("hello", "hi");
        TestUtil.test(future2, true);

        future = client.get("hello");
        TestUtil.test(future, "hi");

        future = client.del("hello");
        TestUtil.test(future, "hi");

        client.close();
    }
}
