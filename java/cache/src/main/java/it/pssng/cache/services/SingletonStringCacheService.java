package it.pssng.cache.services;

import java.util.Map;

public class SingletonStringCacheService {

    private static SingletonStringCacheService instance;
    private GenericCacheService gCacheService = GenericCacheService.getInstance();

    private SingletonStringCacheService() {
    }

    public static SingletonStringCacheService getInstance() {
        if (instance == null)
            instance = new SingletonStringCacheService();

        return instance;
    }

    public void save(String k, String v) {
        gCacheService.put(String.class, k, v);
    }

    public Map<String, String> getAll() {
        try {
            return gCacheService.getCache(String.class);
        } catch (Exception exc) {
            return null;
        }
    }

    public String get(String k) {
        return this.getAll().get(k);
    }

}
