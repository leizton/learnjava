package com.whiker.learn.lkafka;

import com.google.common.collect.ImmutableMap;
import kafka.consumer.ConsumerConfig;
import kafka.consumer.KafkaStream;
import kafka.javaapi.consumer.ConsumerConnector;
import kafka.javaapi.consumer.ZookeeperConsumerConnector;
import kafka.message.MessageAndMetadata;
import kafka.serializer.Decoder;
import org.apache.kafka.common.serialization.IntegerDeserializer;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by whiker on 2017/5/7.
 */
public class Consumer {

    private static final String TOPIC = "wh.lkafka";
    private static final int CONSUMER_NUM = 2;

    private static final IntegerDeserializer KeyDeserializer = new IntegerDeserializer();

    public static void main(String[] args) {
        ConsumerConnector consumerConnector = consumerConnector();
        Map<String, List<KafkaStream<Integer, String>>> consumerMap =
                consumerConnector.createMessageStreams(ImmutableMap.of(TOPIC, CONSUMER_NUM), keyDecoder(), valueDecoder());

        List<KafkaStream<Integer, String>> kafkaStreams = consumerMap.get(TOPIC);
        ExecutorService executor = Executors.newFixedThreadPool(kafkaStreams.size());

        int consumerId = 0;
        for (KafkaStream<Integer, String> stream : kafkaStreams) {
            final int id = consumerId++;
            executor.submit(() -> {
                for (MessageAndMetadata<Integer, String> msg : stream) {
                    System.out.println(id + ">> " + msg.key() + ", " + msg.message());
                }
            });
        }
    }

    private static ConsumerConnector consumerConnector() {
        return new ZookeeperConsumerConnector(new ConsumerConfig(consumerConfig()));
    }

    private static Properties consumerConfig() {
        Properties prop = new Properties();
        prop.put("topic", TOPIC);
        prop.put("zookeeper.connect", "localhost:2181");
        prop.put("group.id", "wh-lkafka-consumer-g0");
        return prop;
    }

    private static Decoder<Integer> keyDecoder() {
        return bytes -> KeyDeserializer.deserialize(TOPIC, bytes);
    }

    private static Decoder<String> valueDecoder() {
        return String::new;
    }
}
