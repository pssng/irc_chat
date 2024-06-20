package it.pssng.cache.kafka;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

import it.pssng.cache.services.SingletonStringCacheService;
import lombok.extern.log4j.Log4j2;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

@Log4j2
public class KafkaStringConsumer {

    private final KafkaConsumer<String, String> consumer;

    public KafkaStringConsumer(String bootstrapServers, String groupId, String topic) {

        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singletonList(topic));

        log.info("Kafka consumer [String] initialized");
    }

    public void consumeMessages() {
        log.info("Kafka [String] is listening..");
        SingletonStringCacheService cacheSvc = SingletonStringCacheService.getInstance();
        while (true) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));
            for (ConsumerRecord<String, String> record : records) {
                log.info("offset = %d, key = %s, value = %s%n", record.offset(), record.key(),
                        record.value().toString());
                cacheSvc.save(record.key(), record.value());
            }
        }
    }

    public void close() {
        log.info("Kafka consumer's listener [String] closed");
        consumer.close();
    }
}
