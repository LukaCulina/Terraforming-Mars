package hr.terraforming.mars.terraformingmars.model;

import hr.terraforming.mars.terraformingmars.enums.TagType;
import hr.terraforming.mars.terraformingmars.enums.TileType;
import hr.terraforming.mars.terraformingmars.factory.CardFactory;
import hr.terraforming.mars.terraformingmars.effects.Effect;
import lombok.Getter;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.function.BiPredicate;

@Getter
public class Card implements Serializable {

    private final String name;
    private final int cost;
    private final String description;
    private transient List<Effect> effects;
    private transient BiPredicate<Player, GameBoard> requirement;
    private final List<TagType> tags;
    private final int victoryPoints;
    private final TileType tileToPlace;

    private Card(Builder builder) {
        this.name = builder.name;
        this.cost = builder.cost;
        this.description = builder.description;
        this.effects = builder.effects;
        this.requirement = builder.requirement;
        this.tags = builder.tags;
        this.victoryPoints = builder.victoryPoints;
        this.tileToPlace = builder.tileToPlace;
    }

    public static class Builder {
        private final String name;
        private final int cost;

        private String description = "";
        private List<Effect> effects = Collections.emptyList();
        private BiPredicate<Player, GameBoard> requirement = (_, _) -> true;
        private List<TagType> tags = Collections.emptyList();
        private int victoryPoints = 0;
        private TileType tileToPlace = null;

        public Builder(String name, int cost) {
            this.name = name;
            this.cost = cost;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder effects(List<Effect> effects) {
            this.effects = effects;
            return this;
        }

        public Builder requirement(BiPredicate<Player, GameBoard> requirement) {
            this.requirement = requirement;
            return this;
        }

        public Builder tags(TagType... tags) {
            this.tags = List.of(tags);
            return this;
        }

        public Builder victoryPoints(int victoryPoints) {
            this.victoryPoints = victoryPoints;
            return this;
        }

        public Builder tileToPlace(TileType tileToPlace) {
            this.tileToPlace = tileToPlace;
            return this;
        }

        public Card build() {
            return new Card(this);
        }
    }

    public void play(Player player, GameManager gameManager) {
        if (effects != null) {
            for (Effect effect : effects) {
                effect.execute(player, gameManager);
            }
        }
    }

    @Serial
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();

        Card originalCard = CardFactory.getCardByName(name);

        if (originalCard != null) {
            effects = originalCard.getEffects();
            requirement = originalCard.getRequirement();
        } else {
            effects = Collections.emptyList();
            requirement = (_, _) -> true;
        }
    }
}