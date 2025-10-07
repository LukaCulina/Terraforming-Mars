package hr.terraforming.mars.terraformingmars.enums;

import hr.terraforming.mars.terraformingmars.model.GameBoard;
import hr.terraforming.mars.terraformingmars.model.Player;
import java.util.function.BiConsumer;

public enum StandardProject {

    SELL_PATENTS("Sell patents", "💰", "Sell cards from hand for 1 MC per card.", 0, null,(_, _) -> {
    }),

    POWER_PLANT("Power Plant", "⚡", "Increase energy production by 1.", 11, null,(player, _) ->
        player.increaseProduction(ResourceType.ENERGY, 1)
    ),

    ASTEROID("Asteroid", "☄","Increase temperature by 1 step (2°C).", 14, null, (player, board) -> {
        if (board.increaseTemperature()) {
            player.increaseTR(1);
        }
    }),

    AQUIFER("Aquifer", "💧", "Place an ocean tile.", 18, TileType.OCEAN, (_, _) -> {
    }),

    GREENERY("Greenery", "🌳","Place a greenery tile and increase oxygen by 1%.",23, TileType.GREENERY, (_, _) -> {
    }),

    CITY("City", "🏙","Place a city tile and increase MC production by 1.", 25, TileType.CITY, (_, _) -> {
    });

    private final String name;
    private final String icon;
    private final String description;
    private final int cost;
    private final BiConsumer<Player, GameBoard> action;
    private final TileType tileType;

    StandardProject(String name,  String icon, String description, int cost, TileType tileType, BiConsumer<Player, GameBoard> action) {
        this.name = name;
        this.icon = icon;
        this.description = description;
        this.cost = cost;
        this.tileType = tileType;
        this.action = action;
    }

    public String getName() { return name; }
    public int getCost() { return cost; }
    public String getDescription() { return description; }
    public String getIcon() { return icon; }

    public void execute(Player player, GameBoard board) {
        action.accept(player, board);
    }

    public boolean requiresTilePlacement() {
        return this.tileType != null;
    }

    public TileType getTileType() {
        return this.tileType;
    }
}