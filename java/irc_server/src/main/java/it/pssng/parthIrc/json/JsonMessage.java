package it.pssng.parthIrc.json;

import com.google.gson.JsonObject;
import lombok.Data;

@Data
public class JsonMessage {

    private JsonObject jsonData;

    // Costruttore con parametri per creare un nuovo messaggio
    public JsonMessage(String sender, String message) {
        jsonData = new JsonObject();
        jsonData.addProperty("sender", sender);
        jsonData.addProperty("message", message);
    }

    // Costruttore vuoto per la deserializzazione
    public JsonMessage() {
        jsonData = new JsonObject();
    }

    // Metodo per ottenere il JsonObject
    public JsonObject getJsonData() {
        return jsonData;
    }

    @Override
    public String toString() {
        return jsonData.toString();
    }

    public static JsonMessage fromJson(JsonObject jsonObject) {
        String sender = jsonObject.get("sender").getAsString();
        String message = jsonObject.get("message").getAsString();
        return new JsonMessage(sender, message);
    }
}
