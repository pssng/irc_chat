package it.pssng.parthIrc.service;

import it.pssng.parthIrc.json.JsonRedis;
import it.pssng.parthIrc.json.JsonMessage;
import lombok.extern.log4j.Log4j2;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.lang.Object;
import java.lang.reflect.Type;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.google.gson.Gson;
import com.google.gson.JsonArray;

@Log4j2
public class CacheService {

    private KafkaProducerService prod;

    public CacheService() {
        log.info("Connecting Kafka producer");
        prod = new KafkaProducerService();
    }

    public void save(String key, String data) {
        log.info("Saving [K:{}|V:{}]", key, data);
        String endpointUrl = "http://localhost:8080/api/v1/cache/get?id=" + key;

        // Crea un client HTTP
        HttpClient client = HttpClient.newHttpClient();

        // Crea una richiesta HTTP GET
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpointUrl))
                .build();

        // Invia la richiesta e gestisci la risposta
        String encodedResponse;
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                // Stampa la risposta ottenuta
                log.info("Response from server:");
                log.info(response.body());
                encodedResponse = decodeFromBase64(response.body());
                log.info(encodedResponse + "  DECODED RESPONSE");

                // Creazione di un oggetto Gson
                Gson gson = new Gson();

                // Definire il tipo della mappa
                Type type = new TypeToken<Map<String, Object>>() {
                }.getType();

                // Parsare la stringa JSON in una mappa
                Map<String, Object> map = gson.fromJson(encodedResponse, type);
                log.info("Parsed JSON object QUI MAP: {}", map);

                // Verifica se la mappa contiene la chiave "list"
                if (map.containsKey("list")) {
                    List<Object> listMessages = (List<Object>) map.get("list");
                    log.info(listMessages + "  LIST MESSAGE");

                    Map<String, Object> mapData = gson.fromJson(data, type);
                    // Ottieni l'array di oggetti dalla mappa
                    List<Object> list = (List<Object>) mapData.get("list");
                    log.info(list.get(0) + "LIST PRIMO ELEMENTO DI MAPDATA");

                    // Aggiungi il nuovo elemento alla lista
                    listMessages.add(list.get(0));
                    log.info(listMessages + "LIST MESSAGE DOPO L'INSERIMENTO ");

                    // Aggiorna la mappa con la lista modificata
                    map.put("list", listMessages);

                    // Converti la mappa aggiornata in una stringa JSON
                    String updatedJson = gson.toJson(map);
                    log.info("Updated JSON: " + updatedJson);

                    // Encode the updated JSON string to Base64
                    String finalData = encodeToBase64(updatedJson);
                    log.info("Encoded data: {} DI FINAL DATA", finalData);

                    // Invia il messaggio al servizio di Kafka
                    prod.sendMessage("irc_chat_cache", key, finalData);
                } else {
                    log.error("The key 'list' does not exist in the map.");
                }
            }
        } catch (IOException | InterruptedException e) {
            log.error("Error during saving [K:{}|V:{}]", key, data, e);
            String encodedData = encodeToBase64(data);
            log.info("Encoded data: {}", encodedData);
            prod.sendMessage("irc_chat_cache", key, encodedData);
        }
    }

    public JsonRedis retrieve(String key) {
        log.info("Retrieving [K:{}]", key);
        String value = "MOCKED"; // Replace with actual Kafka retrieval logic
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

    private String decodeFromBase64(String encodedString) {
        byte[] decodedBytes = Base64.getDecoder().decode(encodedString);
        return new String(decodedBytes);
    }

    private String encodeToBase64(String rawString) {
        byte[] encodedBytes = Base64.getEncoder().encode(rawString.getBytes());
        return new String(encodedBytes);
    }
}
