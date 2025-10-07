package hr.terraforming.mars.terraformingmars.service;

import hr.terraforming.mars.terraformingmars.enums.ResourceType;
import hr.terraforming.mars.terraformingmars.enums.TileType;
import hr.terraforming.mars.terraformingmars.model.GameBoard;
import hr.terraforming.mars.terraformingmars.model.Player;
import hr.terraforming.mars.terraformingmars.model.Tile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public record PlacementService(GameBoard gameBoard) {

    private static final Logger logger = LoggerFactory.getLogger(PlacementService.class);

    public boolean isValidPlacement(TileType placementType, Tile tile, Player player) {
        if (tile.getOwner() != null || tile.getType() != TileType.LAND) {
            return false;
        }

        switch (placementType) {
            case CITY:
                if (gameBoard.isOceanCoordinate(tile.getRow(), tile.getCol())) {
                    return false;
                }
                return gameBoard.getAdjacentTiles(tile).stream().noneMatch(t -> t.getType() == TileType.CITY);

            case GREENERY:
                if (gameBoard.isOceanCoordinate(tile.getRow(), tile.getCol())) {
                    return false;
                }

                List<Tile> ownedTiles = gameBoard.getTiles().stream()
                        .filter(t -> player.equals(t.getOwner()))
                        .toList();

                if (ownedTiles.isEmpty()) {
                    return true;
                }

                boolean hasAnyValidAdjacentSpot = ownedTiles.stream()
                        .flatMap(ownedTile -> gameBoard.getAdjacentTiles(ownedTile).stream())
                        .anyMatch(adjacentTile ->
                                adjacentTile.getOwner() == null &&
                                        adjacentTile.getType() == TileType.LAND &&
                                        !gameBoard.isOceanCoordinate(adjacentTile.getRow(), adjacentTile.getCol())
                        );

                if (hasAnyValidAdjacentSpot) {
                    return gameBoard.getAdjacentTiles(tile).stream()
                            .anyMatch(ownedTiles::contains);
                } else {
                    return true;
                }

            case OCEAN:
                return gameBoard.isOceanCoordinate(tile.getRow(), tile.getCol());

            default:
                return false;
        }
    }

    public void placeOcean(Tile onTile, Player forPlayer) {
        if (gameBoard.getOceansPlaced() < GameBoard.MAX_OCEANS && gameBoard.isOceanCoordinate(onTile.getRow(), onTile.getCol())) {
            onTile.setType(TileType.OCEAN);
            gameBoard.incrementOceansPlaced();
            forPlayer.increaseTR(1);
            gameBoard.notifyUI();

            logger.info("Ocean placed by {}. Total oceans: {}", forPlayer.getName(), gameBoard.getOceansPlaced());
        }
    }

    public void placeGreenery(Tile onTile, Player forPlayer) {
        onTile.setType(TileType.GREENERY);
        onTile.setOwner(forPlayer);
        gameBoard.incrementGreeneryCount();

        if(gameBoard.increaseOxygen()) {
            forPlayer.increaseTR(1);
        }

        gameBoard.notifyUI();

        logger.info("Greenery placed by {} on tile ({}, {}).", forPlayer.getName(), onTile.getRow(), onTile.getCol());
    }

    public void placeCity(Tile onTile, Player forPlayer) {
        onTile.setType(TileType.CITY);
        onTile.setOwner(forPlayer);
        gameBoard.incrementCityCount();
        forPlayer.increaseProduction(ResourceType.MEGACREDITS, 1);
        gameBoard.notifyUI();

        logger.info("City placed by {} on tile ({}, {}).", forPlayer.getName(), onTile.getRow(), onTile.getCol());
    }
}
