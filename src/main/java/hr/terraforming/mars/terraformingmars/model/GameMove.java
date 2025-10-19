package hr.terraforming.mars.terraformingmars.model;

import hr.terraforming.mars.terraformingmars.enums.ActionType;
import hr.terraforming.mars.terraformingmars.enums.TileType;

import java.io.Serializable;
import java.time.LocalDateTime;

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

    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }
    public ActionType getActionType() { return actionType; }
    public void setActionType(ActionType actionType) { this.actionType = actionType; }
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
    public Integer getRow() { return row; }
    public void setRow(Integer row) { this.row = row; }
    public Integer getCol() { return col; }
    public void setCol(Integer col) { this.col = col; }
    public TileType getTileType() { return tileType; }
    public void setTileType(TileType tileType) { this.tileType = tileType; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
