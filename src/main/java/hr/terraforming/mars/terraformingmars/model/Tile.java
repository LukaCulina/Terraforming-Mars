package hr.terraforming.mars.terraformingmars.model;

import hr.terraforming.mars.terraformingmars.enums.TileType;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
public class Tile implements Serializable {
    private final int row;
    private final int col;
    @Setter
    private TileType type;
    @Setter
    private Player owner;

    public Tile(int row, int col, TileType type, Player owner) {
        this.row = row;
        this.col = col;
        this.type = type;
        this.owner = owner;
    }
}
