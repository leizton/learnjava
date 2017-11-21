package com.whiker.learn.spring.dependencyinject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Created by whiker on 16-3-20.
 * 消息服务的存储类
 */
@Component
class MessageServiceStore {
    private final MessageService service;  // 消息服务

    @Autowired
    public MessageServiceStore(MessageService service) {
        this.service = service;
    }

    String getMessage() {
        return service.getMessage();
    }
}
