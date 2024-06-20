import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;

import com.sun.net.httpserver.HttpServer;

import it.pssng.cache.kafka.KafkaStringConsumer;
import it.pssng.cache.services.SingletonStringCacheService;
import it.pssng.cache.utils.BannerUtil;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class Main {
    public static void main(String[] args) throws IOException, InterruptedException, ExecutionException {
        log.info("Starting PSSNG Cache Service..");
        BannerUtil.printBanner();

        // Start HTTP server
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/api/v1/cache/get", (exchange) -> {
            String id = exchange.getRequestURI().getQuery().substring(1).split("&")[0].split("=")[1];
            if (id == null || id.isEmpty()) {
                String response = "Missing 'id' query parameter";
                byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(400, responseBytes.length);
                OutputStream os = exchange.getResponseBody();
                os.write(responseBytes);
                os.close();
            } else {
                SingletonStringCacheService strCacheSvc = SingletonStringCacheService.getInstance();
                String value = strCacheSvc.get(id);
                byte[] responseBytes = value.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, responseBytes.length);
                OutputStream os = exchange.getResponseBody();
                os.write(responseBytes);
                os.close();
            }
        });
        server.setExecutor(null); // creates a default executor
        log.info("Server is listening on port 8080 for request [/api/v1/cache/get?id=<somevalue>]");
        server.start();
        // Start Kafka consumer
        KafkaStringConsumer consumer = new KafkaStringConsumer("localhost:29092", "pssng", "irc_chat_cache");
        consumer.consumeMessages();
    }
}
