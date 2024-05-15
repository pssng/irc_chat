package it.pssng.parthIrc.utils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.util.Base64;

public class Base64Util {

    public static JsonObject convertIntoJsonObject(String jwtToken){
        byte[] decodedBytes = Base64.getDecoder().decode(jwtToken);
        String decodedString = new String(decodedBytes);
        Gson gson = new Gson();
        return gson.fromJson(decodedString, JsonObject.class);
    }

}
