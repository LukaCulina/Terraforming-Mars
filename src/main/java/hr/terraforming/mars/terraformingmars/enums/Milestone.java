package hr.terraforming.mars.terraformingmars.enums;

import hr.terraforming.mars.terraformingmars.model.Player;
import lombok.Getter;

import java.util.function.Predicate;

public enum Milestone {

    TERRAFORMER("Terraformer", "Have a Terraforming Rating of at least 35.",
            player -> player.getTR() >= 35),

    MAYOR("Mayor", "Own at least 3 cities on the board.",
            player -> player.getOwnedCityCount() >= 3),

    GARDENER("Gardener", "Own at least 3 greenery tiles on the board.",
            player -> player.getOwnedGreeneryCount() >= 3),

    BUILDER("Builder", "Have at least 8 building tags in play.",
            player -> player.countTags(TagType.BUILDING) >= 8),

    PLANNER("Planner", "Have at least 16 cards in your hand.",
            player -> player.getHand().size() >= 16);

    @Getter
    private final String name;
    @Getter
    private final String description;
    private final Predicate<Player> requirement;

    Milestone(String name, String description, Predicate<Player> requirement) {
        this.name = name;
        this.description = description;
        this.requirement = requirement;
    }

    public boolean canClaim(Player player) {
        return requirement.test(player);
    }
}