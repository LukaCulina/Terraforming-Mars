/*package hr.terraforming.mars.terraformingmars.model;

import hr.terraforming.mars.terraformingmars.enums.TagType;
import hr.terraforming.mars.terraformingmars.enums.TileType;
import hr.terraforming.mars.terraformingmars.factory.CardFactory;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.function.BiPredicate;

public class Card implements Serializable {
    @FunctionalInterface
    public interface TriConsumer<T, U, V> {
        void accept(T t, U u, V v);
    }

    private final String name;
    private final int cost;
    private final String description;
    private transient TriConsumer<Player, GameBoard, GameManager> effect;
    private transient BiPredicate<Player, GameBoard> requirement;
    private final List<TagType> tags;
    private final int victoryPoints;
    private final TileType tileToPlace;

    private Card(Builder builder) {
        this.name = builder.name;
        this.cost = builder.cost;
        this.description = builder.description;
        this.effect = builder.effect;
        this.requirement = builder.requirement;
        this.tags = builder.tags;
        this.victoryPoints = builder.victoryPoints;
        this.tileToPlace = builder.tileToPlace;
    }

    public static class Builder {
        private final String name;
        private final int cost;

        private String description = "";
        private TriConsumer<Player, GameBoard, GameManager> effect = (p, b, m) -> {};
        private BiPredicate<Player, GameBoard> requirement = (p, b) -> true;
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

        public Builder effect(TriConsumer<Player, GameBoard, GameManager> effect) {
            this.effect = effect;
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

    public BiPredicate<Player, GameBoard> getRequirement() {
        return requirement;
    }

    public String getName() { return name; }
    public int getCost() { return cost; }
    public String getDescription() { return description; }
    public List<TagType> getTags() { return tags; }

    public TileType getTileToPlace() {
        return tileToPlace;
    }

    public void play(Player player, GameBoard board, GameManager gameManager) {
        if (effect != null) effect.accept(player, board, gameManager);
    }

    public int getVictoryPoints() { return victoryPoints; }

    @Serial
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();

        Card originalCard = CardFactory.getCardByName(this.name);

        if (originalCard != null) {
            this.effect = originalCard.effect;
            this.requirement = originalCard.requirement;
        } else {
            this.effect = (p, b, m) -> {};
            this.requirement = (p, b) -> true;
        }
    }
}*/

package hr.terraforming.mars.terraformingmars.model;

import hr.terraforming.mars.terraformingmars.enums.TagType;
import hr.terraforming.mars.terraformingmars.enums.TileType;
import hr.terraforming.mars.terraformingmars.factory.CardFactory;
import hr.terraforming.mars.terraformingmars.model.effects.Effect;
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

        Card originalCard = CardFactory.getCardByName(this.name);

        if (originalCard != null) {
            this.effects = originalCard.getEffects();
            this.requirement = originalCard.getRequirement();
        } else {
            this.effects = Collections.emptyList();
            this.requirement = (_, _) -> true;
        }
    }
}
