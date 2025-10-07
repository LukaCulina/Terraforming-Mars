package hr.terraforming.mars.terraformingmars.model;

import hr.terraforming.mars.terraformingmars.enums.TileType;

import java.io.Serializable;

public class Tile implements Serializable {
    private final int row;
    private final int col;
    private TileType type;
    private Player owner;

    public Tile(int row, int col, TileType type, Player owner) {
        this.row = row;
        this.col = col;
        this.type = type;
        this.owner = owner;
    }

    public int getRow() { return row; }
    public int getCol() { return col; }
    public TileType getType() { return type; }
    public void setType(TileType type) { this.type = type; }
    public Player getOwner() { return owner; }
    public void setOwner(Player owner) { this.owner = owner; }
}
