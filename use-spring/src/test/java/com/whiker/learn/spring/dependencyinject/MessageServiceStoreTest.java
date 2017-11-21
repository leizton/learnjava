package com.whiker.learn.spring.dependencyinject;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.stereotype.Component;

/**
 * Created by whiker on 16-3-20.
 * 测试消息存储类
 */
@Configuration  // 用于xml方式
@ComponentScan  // 用于注解方式
public class MessageServiceStoreTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageServiceStoreTest.class);

    // @Component和@Bean二选一
    // @Component 修饰了 com.whiker.learn.spring.dependencyinject.MessageServiceImpl

    /*
    @Bean
    public MessageService messageService() {
        return new MessageService() {
            @Override
            public String getMessage() {
                return "MessageServiceImpl";
            }
        };
    }
    //*/

    @Test
    public void xmlTest() {
        ApplicationContext context =
                new ClassPathXmlApplicationContext("spring-beans-1.xml");
        MessageServiceStore messageServiceStore = context.getBean(MessageServiceStore.class);
        LOGGER.info("xml: {}", messageServiceStore.getMessage());
    }

    @Test
    public void annotationTest() {
        ApplicationContext context =
                new AnnotationConfigApplicationContext(MessageServiceStoreTest.class);
        MessageServiceStore messageServiceStore = context.getBean(MessageServiceStore.class);
        LOGGER.info("annotation: {}", messageServiceStore.getMessage());
    }
}
