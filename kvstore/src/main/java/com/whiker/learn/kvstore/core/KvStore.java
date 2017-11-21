package com.whiker.learn.kvstore.core;

import com.whiker.learn.kvstore.response.Response;

/**
 * Created by whiker on 2017/3/28.
 */
public interface KvStore {

    void accept(Response response);
}
