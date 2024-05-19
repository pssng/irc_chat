package it.pssng.parthIrc.service;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import it.pssng.parthIrc.config.AppConfig;
import it.pssng.parthIrc.model.CacheObject;
import lombok.extern.log4j.Log4j2;

import java.io.*;
import java.util.Base64;

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

    public void save(String key, CacheObject data) {
        log.info("Saving [K:{}|V:{}]", key, data);
        try {
            String value = serialize(data);
            syncCommands.set(key, value);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Object retrieve(String key) {
        log.info("Retrieving [K:{}]", key);
        String value = syncCommands.get(key);
        if (value != null) {
            try {
                return deserialize(value);
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private String serialize(CacheObject obj) throws IOException {
        log.info("Serializing cache data..");
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream)) {
            objectOutputStream.writeObject(obj);
        }
        byte[] bytes = byteArrayOutputStream.toByteArray();
        return Base64.getEncoder().encodeToString(bytes);
    }

    private Object deserialize(String str) throws IOException, ClassNotFoundException {
        log.info("Deserializing cache data..");
        byte[] bytes = Base64.getDecoder().decode(str);
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
                ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream)) {
            return objectInputStream.readObject();
        }
    }

    public void close() {
        log.info("Closing redis connection");
        connection.close();
        redisClient.shutdown();
    }
}
