package it.pssng.parthIrc.model;

import it.pssng.parthIrc.exceptions.IllegalPssngCommandException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessionData {

    private String fiscalCode;
    private UserRole role;

    public SessionData(String command) throws IllegalPssngCommandException {
        String[] parts = command.split("\\$\\$");
        if (parts.length >= 2 && command.startsWith("/")) {
            fiscalCode = parts[0].substring(1);
            role = Integer.valueOf(parts[1]) == 0 ? UserRole.USER : UserRole.ADMIN;
        } else {
            throw new IllegalPssngCommandException("Illegal command \"" + command + "\"");
        }
    }

}
