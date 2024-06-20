import it.pssng.cache.kafka.KafkaStringConsumer;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class Main {
    public static void main(String[] args) {
        log.info("Starting PSSNG Cache Service..");
        KafkaStringConsumer consumer = new KafkaStringConsumer("localhost:9999", "pssng", "cache_loader");
        consumer.consumeMessages();
    }
}
