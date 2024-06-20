package it.pssng.cache.services;

import java.util.HashMap;
import java.util.Map;

public class GenericCacheService {

    /*
     * Implementazione di un Singleton (design pattern)
     */
    private static GenericCacheService instance; // inserisco un'istanza della classe che sto scrivendo come attributo della classe stessa

    private Map<Class<?>, Map<String, Object>> myCache; // creo una mappa le cui chiavi sono il tipo di dato salvato all' interno dei valori delle mappe interne

    //costruttore privato che sarà richiamato nella getInstance()
    private GenericCacheService() {
        myCache = new HashMap<>();
    }

    /*
     * Ritorna l'istanza:
     * SE VUOTA: crea una nuova istanza di instance
     * SE PIENA: restituisce l'istanza già precedentemente istanziata
     */
    public static GenericCacheService getInstance() {
        if (instance == null)
            instance = new GenericCacheService();

        return instance;
    }

    /*
     * Inserisce l'oggetto nella cache corretta
     */
    public <T> void put(Class<T> clazz, String key, T object) {
        if (!myCache.containsKey(clazz)) 
            myCache.put(clazz, new HashMap<>());
        myCache.get(clazz).put(key, object);
    }

    /*
     * Recupera l'intera cache relativa ad un tipo di dato
     */
    @SuppressWarnings("unchecked")
    public <T> Map<String, T> getCache(Class<T> clazz) throws Exception {
        if (!myCache.containsKey(clazz)) 
            throw new Exception("No cache found for class " + clazz.getName());
        return (Map<String, T>) myCache.get(clazz);
    }



}
