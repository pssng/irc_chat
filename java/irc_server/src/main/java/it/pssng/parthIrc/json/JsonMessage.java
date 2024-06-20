package it.pssng.parthIrc.json;

import com.google.gson.JsonObject;

import lombok.Data;

@Data
public class JsonMessage {

    JsonObject jsonData = new JsonObject();

    public JsonMessage(String sender, String message) {
        jsonData.addProperty("sender", sender);
        jsonData.addProperty("message", message);
    }

    public String toString() {
        return jsonData.toString();
    }

}
