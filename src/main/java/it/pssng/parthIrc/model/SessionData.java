package it.pssng.parthIrc.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessionData {

    private String fiscalCode;
    private UserRole role;
    private String msg;

    public SessionData(String command) {
        String[] parts = command.split("\\$\\$");
        if (parts.length >= 3) {
            fiscalCode = parts[0];
            role = Integer.valueOf(parts[1]) == 0 ? UserRole.USER : UserRole.ADMIN;
            msg = parts[2];
        } else {
            // Gestione dell'errore o comportamento predefinito in caso di stringa non
            // valida
            fiscalCode = "";
            role = UserRole.USER;
            msg = "";
            // Oppure puoi lanciare un'eccezione
            // throw new IllegalArgumentException("Stringa di comando non valida: " +
            // command);
        }
    }

}
