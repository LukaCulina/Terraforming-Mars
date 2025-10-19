package hr.terraforming.mars.terraformingmars.model;

import hr.terraforming.mars.terraformingmars.enums.Milestone;
import hr.terraforming.mars.terraformingmars.enums.TileType;
import hr.terraforming.mars.terraformingmars.service.PlacementService;
import hr.terraforming.mars.terraformingmars.util.HexGridHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

public class GameBoard implements Serializable {

    private static final Logger logger = LoggerFactory.getLogger(GameBoard.class);

    private int oxygenLevel;
    private int temperature;
    private int oceansPlaced;
    private int greeneryCount;
    private int cityCount;
    private final Map<Milestone, Player> claimedMilestones = new EnumMap<>(Milestone.class);
    public static final int MAX_MILESTONES = 3;
    private boolean isFinalGeneration = false;

    public static final int MAX_OXYGEN = 14;
    public static final int MAX_TEMPERATURE = 8;
    public static final int MIN_TEMPERATURE = -30;
    public static final int MAX_OCEANS = 9;

    private transient Runnable onGlobalParametersChanged;
    private transient PlacementService placementService;

    private final List<Tile> tiles = new ArrayList<>();
    private static final int[][] OCEAN_POSITIONS = {
            {0, 1}, {0, 3}, {0, 4},
            {1, 5},
            {3, 7},
            {4, 3}, {4, 4}, {4, 5},
            {5, 5}, {5, 6}, {5, 7},
            {8, 4}
    };

    public GameBoard() {
        this.placementService = new PlacementService(this);
        initBoard();
        this.oxygenLevel = 0;
        temperature = MIN_TEMPERATURE;
        this.oceansPlaced = 0;
        this.greeneryCount = 0;
        this.cityCount = 0;
        claimedMilestones.clear();
    }

    @Serial
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        this.placementService = new PlacementService(this);
    }

    private void initBoard() {
        int[] hexesInRow = HexGridHelper.getHexesInRow();
        for (int row = 0; row < hexesInRow.length; row++) {
            int hexesThisRow = hexesInRow[row];
            for (int col = 0; col < hexesThisRow; col++) {
                tiles.add(new Tile(row, col, TileType.LAND, null));
            }
        }
    }

    public boolean isValidPlacement(TileType placementType, Tile tile, Player player) {
        return placementService.isValidPlacement(placementType, tile, player);
    }

    public void placeOcean(Tile onTile, Player forPlayer) {
        placementService.placeOcean(onTile, forPlayer);
    }

    public void placeGreenery(Tile onTile, Player forPlayer) {
        placementService.placeGreenery(onTile, forPlayer);
    }

    public void placeCity(Tile onTile, Player forPlayer) {
        placementService.placeCity(onTile, forPlayer);
    }

    public void incrementOceansPlaced() { this.oceansPlaced++; }
    public void incrementGreeneryCount() { this.greeneryCount++; }
    public void incrementCityCount() { this.cityCount++; }

    public boolean isFinalGeneration() {
        return isFinalGeneration;
    }

    private void checkEndGameTrigger() {
        if (isFinalGeneration) return;

        if (getOxygenLevel() >= MAX_OXYGEN &&
                getTemperature() >= MAX_TEMPERATURE &&
                getOceansPlaced() >= MAX_OCEANS) {

            this.isFinalGeneration = true;

            logger.info("GAME END TRIGGERED!: All global parameters are at maximum. The game will end after this generation.");
        }
    }

    public boolean canPlaceOcean() {
        return getOceansPlaced() < MAX_OCEANS;
    }

    public boolean isOceanCoordinate(int row, int col) {
        for (int[] pos : OCEAN_POSITIONS) {
            if (pos[0] == row && pos[1] == col) {
                return true;
            }
        }
        return false;
    }

    public List<Tile> getTiles() { return tiles; }

    public int[] getHexesInRow() { return HexGridHelper.getHexesInRow(); }

    public int getOxygenLevel() { return oxygenLevel; }
    public boolean increaseOxygen() {
        if (getOxygenLevel() < MAX_OXYGEN) {
            oxygenLevel++;
            notifyUI();
            checkEndGameTrigger();
            return true;
        }
        return false;
    }

    public int getTemperature() { return temperature; }
    public boolean increaseTemperature() {
        if (temperature < MAX_TEMPERATURE) {
            temperature += 2;
            if (temperature > MAX_TEMPERATURE) {
                temperature = MAX_TEMPERATURE;
            }
            notifyUI();
            checkEndGameTrigger();
            return true;
        }
        return false;
    }

    public int getOceansPlaced() { return oceansPlaced; }

    public void notifyUI() {
        if (onGlobalParametersChanged != null) {
            onGlobalParametersChanged.run();
        }
    }

    public void setOnGlobalParametersChanged(Runnable callback) {
        this.onGlobalParametersChanged = callback;
    }

    public boolean claimMilestone(Milestone milestone, Player player) {
        if (claimedMilestones.size() >= MAX_MILESTONES) {
            logger.warn("Cannot claim milestone: maximum number of milestones ({}) has been reached.", MAX_MILESTONES);
            return false;
        }

        if (claimedMilestones.containsKey(milestone)) {
            logger.warn("Milestone '{}' has already been claimed.", milestone.getName());
            return false;
        }

        if (!milestone.canClaim(player)) {
            logger.warn("{} does not meet the requirements for milestone '{}'.", player.getName(), milestone.getName());
            return false;
        }
        claimedMilestones.put(milestone, player);
        player.addClaimedMilestone(milestone);

        logger.info("{} has claimed the milestone: {}!", player.getName(), milestone.getName());
        return true;
    }

    public Map<Milestone, Player> getClaimedMilestones() {
        return Collections.unmodifiableMap(claimedMilestones);
    }

    public List<Tile> getAdjacentTiles(Tile centerTile) {
        return HexGridHelper.getAdjacentTiles(centerTile, this.tiles);
    }

    public Tile getTileAt(int row, int col) {
        return tiles.stream()
                .filter(t -> t.getRow() == row && t.getCol() == col)
                .findFirst()
                .orElse(null);
    }
}
