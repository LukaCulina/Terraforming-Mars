package hr.terraforming.mars.terraformingmars.model;

import hr.terraforming.mars.terraformingmars.enums.Milestone;
import hr.terraforming.mars.terraformingmars.enums.ResourceType;
import hr.terraforming.mars.terraformingmars.enums.TagType;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.*;

public class PlayerState implements Serializable {

    private int mcValue;
    private int trValue;
    private int tilePointsValue;
    private final Map<ResourceType, Integer> resourceValues = new EnumMap<>(ResourceType.class);
    private final Map<ResourceType, Integer> productionValues = new EnumMap<>(ResourceType.class);

    private transient IntegerProperty tilePoints;
    private transient IntegerProperty mc;
    private transient IntegerProperty tr;
    private transient Map<ResourceType, IntegerProperty> resources;
    private transient Map<ResourceType, IntegerProperty> production;

    private final List<Milestone> claimedMilestones = new ArrayList<>();
    private final List<Card> hand = new ArrayList<>();
    private final List<Card> played = new ArrayList<>();

    public PlayerState() {
        this.trValue = 20;
        initializeTransientProperties();
    }

    private void initializeTransientProperties() {
        this.resources = new EnumMap<>(ResourceType.class);
        this.production = new EnumMap<>(ResourceType.class);

        this.tr = new SimpleIntegerProperty(this.trValue);
        this.mc = new SimpleIntegerProperty(this.mcValue);
        this.tilePoints = new SimpleIntegerProperty(this.tilePointsValue);

        for (ResourceType type : ResourceType.values()) {
            resources.put(type, new SimpleIntegerProperty(resourceValues.getOrDefault(type, 0)));
            production.put(type, new SimpleIntegerProperty(productionValues.getOrDefault(type, 0)));
        }
    }

    @Serial
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        initializeTransientProperties();
    }

    @Serial
    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        this.trValue = this.tr.get();
        this.mcValue = this.mc.get();
        this.tilePointsValue = this.tilePoints.get();

        resources.forEach((key, value) -> resourceValues.put(key, value.get()));
        production.forEach((key, value) -> productionValues.put(key, value.get()));

        out.defaultWriteObject();
    }

    public IntegerProperty tilePointsProperty() { return tilePoints; }
    public IntegerProperty mcProperty() { return mc; }
    public IntegerProperty trProperty() { return tr; }
    public IntegerProperty resourceProperty(ResourceType type) { return resources.get(type); }
    public IntegerProperty productionProperty(ResourceType type) { return production.get(type); }
    public List<Milestone> getClaimedMilestones() { return claimedMilestones; }
    public List<Card> getHand() { return hand; }
    public List<Card> getPlayed() { return played; }
    public Map<ResourceType, IntegerProperty> getProductionMap() {
        return Collections.unmodifiableMap(production);
    }

    public int getCardCost(Card card, Corporation corporation) {
        int currentCost = card.getCost();
        if (corporation == null) {
            return currentCost;
        }

        String corpName = corporation.name();

        if (corpName.equals("Credicor")) {
            currentCost -= 1;
        }
        else if (corpName.equals("Mining Guild") && card.getTags().contains(TagType.BUILDING)) {
            currentCost -= 2;
        }
        else if (corpName.equals("Phobolog") && card.getTags().contains(TagType.SPACE)) {
            currentCost -= 4;
        }
        else if (corpName.equals("Teractor") && card.getTags().contains(TagType.EARTH)) {
            currentCost -= 3;
        }
        else if (corpName.equals("Inventrix") && card.getTags().contains(TagType.SCIENCE)) {
            currentCost -= 2;
        }
        else if (corpName.equals("Thorgate") && card.getTags().contains(TagType.ENERGY)) {
            currentCost -= 3;
        }

        return Math.max(0, currentCost);
    }
}
