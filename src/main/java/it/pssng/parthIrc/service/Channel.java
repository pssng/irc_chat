package it.pssng.parthIrc.service;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Channel {
   private String channel;
   private Boolean flag = false;

    @Override
    public String toString() {
        return channel;
    }
}
