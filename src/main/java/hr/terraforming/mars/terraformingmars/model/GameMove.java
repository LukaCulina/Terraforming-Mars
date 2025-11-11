package hr.terraforming.mars.terraformingmars.model;

import hr.terraforming.mars.terraformingmars.enums.ActionType;
import hr.terraforming.mars.terraformingmars.enums.TileType;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

@Setter
@Getter
public class GameMove implements Serializable {
    private String playerName;
    private ActionType actionType;
    private String details;
    private Integer row;
    private Integer col;
    private TileType tileType;
    private LocalDateTime timestamp;

    public GameMove(String playerName, ActionType actionType, String details, LocalDateTime timestamp) {
        this.playerName = playerName;
        this.actionType = actionType;
        this.details = details;
        this.timestamp = timestamp;
    }

    public GameMove(String playerName, ActionType actionType, String details, Integer row, Integer col, TileType tileType, LocalDateTime timestamp) {
        this.playerName = playerName;
        this.actionType = actionType;
        this.details = details;
        this.row = row;
        this.col = col;
        this.tileType = tileType;
        this.timestamp = timestamp;
    }

    public GameMove() {}

}
