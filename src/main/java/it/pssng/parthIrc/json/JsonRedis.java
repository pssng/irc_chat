package it.pssng.parthIrc.json;

import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import com.google.gson.JsonArray;

import it.pssng.parthIrc.service.CacheService;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class JsonRedis {

    private String chatIdData;
    private List<JsonMessage> messages = new ArrayList<>();
    private transient CacheService cacheService;

    public JsonRedis(String chatId) {
        chatIdData = chatId;
        cacheService = new CacheService();
    }

    public String toString() {
        JsonObject returnJson = new JsonObject();

        returnJson.addProperty("chat-id", chatIdData);

        JsonArray messagesArray = new JsonArray();
        for (JsonMessage message : messages) {
            messagesArray.add(message.getJsonData());
        }
        returnJson.add("list", messagesArray);

        return returnJson.toString();
    }

    public void loadMessage(JsonMessage jsonMessage) {
        this.messages.add(jsonMessage);
    }

    public void saveToCache() {
        String key = "chat:" + chatIdData;
        String jsonString = this.toString();
        log.info("Saving JsonRedis to cache with key: {} and data: {}", key, jsonString);
        cacheService.save(key, jsonString);
    }

    public static JsonRedis loadFromCache(String chatId) {
        CacheService cacheService = new CacheService();
        return cacheService.retrieve(chatId);
    }
}
