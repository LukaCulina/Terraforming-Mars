package hr.terraforming.mars.terraformingmars.model;

import java.io.Serializable;
import java.time.LocalDateTime;

public record GameMove(String playerName, String actionDescription, LocalDateTime timestamp, Integer row, Integer col) implements Serializable {

    public GameMove(String playerName, String actionDescription, LocalDateTime timestamp) {
        this(playerName, actionDescription, timestamp, null, null);
    }
}
