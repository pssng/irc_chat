package it.pssng.parthIrc.service;


import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import lombok.extern.log4j.Log4j2;

import java.util.Properties;

@Log4j2
public class KafkaProducerService {
    private final Producer<String, String> producer;

    public KafkaProducerService() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:29092");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        producer = new KafkaProducer<>(props);
    }

    public void sendMessage(String topic, String key, String value) {
        ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, value);
        producer.send(record, (metadata, exception) -> {
            if (exception == null) {
                log.info("Message sent to partition %d at offset %d%n", metadata.partition(), metadata.offset());
            } else {
                exception.printStackTrace();
            }
        });
    }

    public void close() {
        producer.close();
    }
}
