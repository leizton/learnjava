package com.whiker.learn.lkafka;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.Properties;

/**
 * 1. 启动zk
 * bin/zookeeper-server-start.sh config/zookeeper.properties
 * <p>
 * 2. 启动server(broker)
 * bin/kafka-server-start.sh config/server.properties
 * <p>
 * 3. 创建topic, partition数是3
 * bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions 3 --topic wh.lkafka
 * 查看topic
 * bin/kafka-topics.sh --list --zookeeper localhost:2181
 * <p>
 * Created by whiker on 2017/5/7.
 */
public class Producer {

    private static final String TOPIC = "wh.lkafka";

    public static void main(String[] args) {
        KafkaProducer<Integer, byte[]> producer = producer();
        for (int key = 0; key < 10; ++key) {
            producer.send(record(TOPIC, key, ("msg-" + key).getBytes()));
        }
        producer.flush();
        producer.close();
    }

    private static KafkaProducer<Integer, byte[]> producer() {
        return new KafkaProducer<>(produceConfig());
    }

    private static Properties produceConfig() {
        Properties prop = new Properties();
        prop.put("bootstrap.servers", "localhost:9092");
        prop.put("key.serializer", "org.apache.kafka.common.serialization.IntegerSerializer");
        prop.put("value.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer");
        prop.put("producer.type", "sync");
        prop.put("batch.num.messages", 1);
        return prop;
    }

    private static ProducerRecord<Integer, byte[]> record(String topic, int key, byte[] value) {
        return new ProducerRecord<>(topic, key, value);
    }
}
