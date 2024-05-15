package it.pssng.parthIrc.model;

import com.google.gson.JsonObject;
import it.pssng.parthIrc.utils.Base64Util;
import lombok.Data;

@Data
public class UserDetails {

    private String generals;
    private String fiscalCode;
    private UserRole role;

    public UserDetails (String jwtToken){
        JsonObject userData = Base64Util.convertIntoJsonObject(jwtToken);
        //TODO: estrarre i dati dal token per assegnare nell'oggetto
    }

    public UserDetails(String generals, String fiscalCode, UserRole role) {
        this.generals = generals;
        this.fiscalCode = fiscalCode;
        this.role = role;
    }
}
