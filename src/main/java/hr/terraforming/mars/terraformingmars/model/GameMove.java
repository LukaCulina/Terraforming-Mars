package hr.terraforming.mars.terraformingmars.model;

import hr.terraforming.mars.terraformingmars.enums.ActionType;
import hr.terraforming.mars.terraformingmars.enums.TileType;

import java.io.Serializable;
import java.time.LocalDateTime;

public record GameMove(String playerName, ActionType actionType, String details, Integer row, Integer col,
                       TileType tileType, LocalDateTime timestamp) implements Serializable {
    public GameMove(String playerName, ActionType actionType, String details, LocalDateTime timestamp) {
        this(playerName, actionType, details, null, null, null, timestamp);
    }
}