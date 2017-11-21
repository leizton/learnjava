package com.whiker.learn.spring.dependencyinject;

import org.springframework.stereotype.Component;

/**
 * Created by whiker on 16-3-27.
 * 消息服务的实现类
 */
@Component
public class MessageServiceImpl implements MessageService {
    @Override
    public String getMessage() {
        return "MessageServiceImpl";
    }
}
