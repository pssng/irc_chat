package it.pssng.parthIrc.service;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import it.pssng.parthIrc.config.AppConfig;
import it.pssng.parthIrc.json.JsonRedis;
import it.pssng.parthIrc.json.JsonMessage;
import lombok.extern.log4j.Log4j2;

import java.util.Base64;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonReader;
import java.io.StringReader;
import com.google.gson.JsonArray;

@Log4j2
public class CacheService {

    private RedisClient redisClient;
    private StatefulRedisConnection<String, String> connection;
    private RedisCommands<String, String> syncCommands;

    public CacheService() {
        log.info("Creating redis connection");
        AppConfig config = new AppConfig();
        redisClient = RedisClient.create(config.getProperty("redis.connection.datasource"));
        connection = redisClient.connect();
        syncCommands = connection.sync();
    }

    public void save(String key, String data) {
        log.info("Saving [K:{}|V:{}]", key, data);
        String encodedData = encodeToBase64(data);
        log.info("Encoded data: {}", encodedData);
        syncCommands.set(key, encodedData);
    }

    public JsonRedis retrieve(String key) {
        log.info("Retrieving [K:{}]", key);
        String value = syncCommands.get(key);
        if (value != null) {
            try {
                String decodedData = decodeFromBase64(value);
                log.info("Decoded data: {}", decodedData);
                JsonReader reader = new JsonReader(new StringReader(decodedData));
                reader.setLenient(true);
                JsonObject jsonObject = JsonParser.parseReader(reader).getAsJsonObject();

                String chatId = jsonObject.get("chat-id").getAsString();
                JsonRedis jsonRedis = new JsonRedis(chatId);
                
                JsonArray messagesArray = jsonObject.getAsJsonArray("list");
                for (int i = 0; i < messagesArray.size(); i++) {
                    JsonObject messageObject = messagesArray.get(i).getAsJsonObject();
                    String sender = messageObject.get("sender").getAsString();
                    String message = messageObject.get("message").getAsString();
                    JsonMessage jsonMessage = new JsonMessage(sender, message);
                    jsonRedis.loadMessage(jsonMessage);
                }
                return jsonRedis;
            } catch (JsonSyntaxException e) {
                log.error("Error parsing JSON from cache: {}", e.getMessage());
            } catch (IllegalStateException e) {
                log.error("Expected a JSON Object but found different data: {}", e.getMessage());
            }
        }
        return null;
    }

    public boolean exists(String key) {
        return retrieve(key) != null;
    }

    private String encodeToBase64(String data) {
        return Base64.getEncoder().encodeToString(data.getBytes());
    }

    private String decodeFromBase64(String base64Data) {
        byte[] decodedBytes = Base64.getDecoder().decode(base64Data);
        return new String(decodedBytes);
    }

    public void close() {
        log.info("Closing redis connection");
        connection.close();
        redisClient.shutdown();
    }
}
