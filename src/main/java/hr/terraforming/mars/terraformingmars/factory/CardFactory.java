package hr.terraforming.mars.terraformingmars.factory;

import hr.terraforming.mars.terraformingmars.enums.TileType;
import hr.terraforming.mars.terraformingmars.model.Card;
import hr.terraforming.mars.terraformingmars.enums.ResourceType;
import hr.terraforming.mars.terraformingmars.enums.TagType;

import java.util.ArrayList;
import java.util.List;

public class CardFactory {

    private static final List<Card> allCards = initCards();

    private CardFactory() {}

    public static List<Card> getAllCards() {
        return new ArrayList<>(allCards);
    }

    private static List<Card> initCards() {
        List<Card> cards = new ArrayList<>();

        cards.add(new Card.Builder("Research Lab", 11)
                .tags(TagType.SCIENCE, TagType.BUILDING).description("Draw 2 cards immediately.").effect((p, b, m) -> p.addCardsToHand(m.drawCards(2))).victoryPoints(1).build());

        cards.add(new Card.Builder("AI Central", 18)
                .tags(TagType.SCIENCE, TagType.BUILDING).description("Gain 1 MC for each science tag you have.").effect((p, b, m) -> p.addMC(p.countTags(TagType.SCIENCE))).victoryPoints(1).build());

        cards.add(new Card.Builder("Advanced Alloys", 9)
                .tags(TagType.SCIENCE).description("Gain 3 steel and 3 titanium immediately.").effect((p, b, m) -> {p.addResource(ResourceType.STEEL, 3);p.addResource(ResourceType.TITANIUM, 3);}).victoryPoints(0).build());

        cards.add(new Card.Builder("Medical Lab", 13)
                .tags(TagType.SCIENCE, TagType.BUILDING).description("Gain 2 MC for each Earth tag you have.").effect((p, b, m) -> p.addMC(2 * p.countTags(TagType.EARTH))).victoryPoints(1).build());

        cards.add(new Card.Builder("Power Plant", 4)
                .tags(TagType.BUILDING, TagType.ENERGY).description("Increase your energy production by 1.").effect((p, b, m) -> p.increaseProduction(ResourceType.ENERGY, 1)).build());

        cards.add(new Card.Builder("Steelworks", 7)
                .tags(TagType.BUILDING).description("Increase your steel production by 1 and energy production by 1.").effect((p, b, m) -> {p.increaseProduction(ResourceType.STEEL, 1);p.increaseProduction(ResourceType.ENERGY, 1);}).build());

        cards.add(new Card.Builder("Ore Processor", 13)
                .tags(TagType.BUILDING).description("Requirement: Energy production >= 1. Effect: Decrease energy production by 1, increase steel production by 2.").requirement((p, b) -> p.getProduction(ResourceType.ENERGY) >= 1).effect((p, b, m) -> {p.increaseProduction(ResourceType.ENERGY, -1);p.increaseProduction(ResourceType.STEEL, 2);}).victoryPoints(1).build());

        cards.add(new Card.Builder("Fusion Power", 14)
                .tags(TagType.SCIENCE, TagType.ENERGY).description("Requirement: You have at least 2 science tags. Effect: Increase energy production by 3.").requirement((p, b) -> p.countTags(TagType.SCIENCE) >= 2).effect((p, b, m) -> p.increaseProduction(ResourceType.ENERGY, 3)).build());

        cards.add(new Card.Builder("Industrial Center", 4)
                .tags(TagType.BUILDING).description("Increase your steel production by 1.").effect((p, b, m) -> p.increaseProduction(ResourceType.STEEL, 1)).build());

        cards.add(new Card.Builder("Asteroid Mining", 30)
                .tags(TagType.SPACE, TagType.JOVIAN).description("Increase your titanium production by 2.").effect((p, b, m) -> p.increaseProduction(ResourceType.TITANIUM, 2)).victoryPoints(2).build());

        cards.add(new Card.Builder("Space Elevator", 27)
                .tags(TagType.SPACE, TagType.BUILDING).description("Gain 15 MC immediately and increase your titanium production by 1.").effect((p, b, m) -> {p.addMC(15);p.increaseProduction(ResourceType.TITANIUM, 1);}).victoryPoints(2).build());

        cards.add(new Card.Builder("Ganymede Colony", 20)
                .tags(TagType.SPACE, TagType.JOVIAN).description("Increase your MC production by 2.").effect((p, b, m) -> p.increaseProduction(ResourceType.MEGACREDITS, 2)).victoryPoints(1).build());

        cards.add(new Card.Builder("Callisto Penal Mines", 24)
                .tags(TagType.SPACE, TagType.JOVIAN).description("Increase your MC production by 3.").effect((p, b, m) -> p.increaseProduction(ResourceType.MEGACREDITS, 3)).victoryPoints(1).build());

        cards.add(new Card.Builder("Io Mining Industries", 41)
                .tags(TagType.SPACE, TagType.JOVIAN).description("Increase MC production by 1 for each Jovian tag you have.").effect((p, b, m) -> p.increaseProduction(ResourceType.MEGACREDITS, p.countTags(TagType.JOVIAN))).victoryPoints(2).build());

        cards.add(new Card.Builder("Deimos Down", 31)
                .tags(TagType.SPACE)
                .description("Increase temperature by 1 step and gain 4 steel immediately.")
                .effect((p, b, m) -> {
                    b.increaseTemperature();
                    p.addResource(ResourceType.STEEL, 4);
                })
                .build());

        cards.add(new Card.Builder("Great Dam", 12)
                .tags(TagType.BUILDING, TagType.ENERGY).description("Requirement: At least 4 oceans placed. Effect: Increase energy production by 2.").requirement((p, b) -> b.getOceansPlaced() >= 4).effect((p, b, m) -> p.increaseProduction(ResourceType.ENERGY, 2)).victoryPoints(1).build());

        cards.add(new Card.Builder("Kelp Farming", 18)
                .tags(TagType.PLANT).description("Increase plant production by 2 and MC production by 1.").effect((p, b, m) -> {p.increaseProduction(ResourceType.PLANTS, 2);p.increaseProduction(ResourceType.MEGACREDITS, 1);
                }).victoryPoints(1).build());

        cards.add(new Card.Builder("Nitrogen-Rich Asteroid", 23)
                .tags(TagType.SPACE).description("Increase your TR by 2 and plant production by 2.").effect((p, b, m) -> {p.increaseTR(2);p.increaseProduction(ResourceType.PLANTS, 2);}).build());

        cards.add(new Card.Builder("Sabotage", 1)
                .description("Gain 5 MC.").effect((p, b, m) -> p.addMC(5)).build());

        cards.add(new Card.Builder("Hackers", 4)
                .description("Gain 2 MC for each opponent in the game.").effect((p, b, m) -> p.addMC(2 * (m.getPlayers().size() - 1))).build());

        cards.add(new Card.Builder("Investment Loan", 3)
                .tags(TagType.EARTH).description("Gain 10 MC, but decrease your MC production by 1.").effect((p, b, m) -> {p.addMC(10);p.increaseProduction(ResourceType.MEGACREDITS, -1);}).build());

        cards.add(new Card.Builder("Media Group", 6)
                .tags(TagType.EARTH).description("Gain 1 MC for every two Earth tags you have (including this one).").effect((p, b, m) -> p.addMC((p.countTags(TagType.EARTH) + 1) / 2)).victoryPoints(1).build());

        cards.add(new Card.Builder("Business Network", 4)
                .tags(TagType.EARTH).description("Increase your MC production by 1 and draw 1 card.").effect((p, b, m) -> {p.increaseProduction(ResourceType.MEGACREDITS, 1);p.addCardsToHand(m.drawCards(1));}).build());

        cards.add(new Card.Builder("Windmill", 6)
                .tags(TagType.ENERGY, TagType.BUILDING).description("Requirement: Oxygen at 4% or more. Effect: Increase your energy production by 1.").requirement((p, b) -> b.getOxygenLevel() >= 4).effect((p, b, m) -> p.increaseProduction(ResourceType.ENERGY, 1)).build());

        cards.add(new Card.Builder("Robotics Factory", 6)
                .tags(TagType.BUILDING, TagType.SCIENCE).description("Increase your steel production by 1.").effect((p, b, m) -> p.increaseProduction(ResourceType.STEEL, 1)).victoryPoints(1).build());

        cards.add(new Card.Builder("Greenhouse", 6)
                .tags(TagType.PLANT, TagType.BUILDING).description("Increase your Heat production by 1.").effect((p, b, m) -> p.increaseProduction(ResourceType.HEAT, 1)).build());

        cards.add(new Card.Builder("Lichen", 5)
                .tags(TagType.PLANT).description("Increase your titanium production by 1.").effect((p, b, m) -> p.increaseProduction(ResourceType.TITANIUM, 1)).build());

        cards.add(new Card.Builder("Microbe Cultivation", 7)
                .tags(TagType.PLANT, TagType.SCIENCE).description("Increase your plant production by 2.").effect((p, b, m) -> p.increaseProduction(ResourceType.PLANTS, 2)).build());

        cards.add(new Card.Builder("Experimental Plant", 4)
                .tags(TagType.PLANT, TagType.SCIENCE).description("Increase your plant production by 1.").effect((p, b, m) -> p.increaseProduction(ResourceType.PLANTS, 1)).victoryPoints(1).build());

        cards.add(new Card.Builder("Forest Expansion", 13)
                .tags(TagType.PLANT, TagType.EARTH).description("Increase your plant production by 1 and MC production by 1.").effect((p, b, m) -> {p.increaseProduction(ResourceType.PLANTS, 1);p.increaseProduction(ResourceType.MEGACREDITS, 1);}).victoryPoints(1).build());

        cards.add(new Card.Builder("Heating", 6)
                .tags(TagType.ENERGY).description("Increase your heat production by 2.").effect((p, b, m) -> p.increaseProduction(ResourceType.HEAT, 2)).build());

        cards.add(new Card.Builder("Asteroid", 14)
                .tags(TagType.SPACE).description("Increase temperature by 1 step (you gain 1 TR).").effect((p, b, m) -> {b.increaseTemperature();p.increaseTR(1);}).build());

        cards.add(new Card.Builder("Terraforming Hub", 13)
                .tags(TagType.BUILDING, TagType.SCIENCE).description("Increase your MC production by 5.").effect((p, b, m) -> p.increaseProduction(ResourceType.MEGACREDITS, 5)).victoryPoints(1).build());

        cards.add(new Card.Builder("Research", 11)
                .tags(TagType.SCIENCE).description("Draw 2 cards.").effect((p, b, m) -> p.addCardsToHand(m.drawCards(2))).victoryPoints(1).build());

        cards.add(new Card.Builder("Patents", 3)
                .tags(TagType.SCIENCE).description("Draw 1 card.").effect((p, b, m) -> p.addCardsToHand(m.drawCards(1))).build());

        cards.add(new Card.Builder("Terraforming Contract", 0)
                .tags(TagType.EARTH).description("Increase your MC production by 2.").effect((p, b, m) -> p.increaseProduction(ResourceType.MEGACREDITS, 2)).build());

        cards.add(new Card.Builder("Ecology Center", 11)
                .tags(TagType.PLANT, TagType.BUILDING, TagType.SCIENCE).description("Increase your plant production by 1.").effect((p, b, m) -> p.increaseProduction(ResourceType.PLANTS, 1)).victoryPoints(1).build());

        cards.add(new Card.Builder("University", 8)
                .tags(TagType.BUILDING, TagType.SCIENCE).description("Increase your MC production by 1.").effect((p, b, m) -> p.increaseProduction(ResourceType.MEGACREDITS, 1)).victoryPoints(1).build());

        cards.add(new Card.Builder("Jovian Outpost", 15)
                .tags(TagType.JOVIAN, TagType.SPACE).description("Increase your MC production by 1.").effect((p, b, m) -> p.increaseProduction(ResourceType.MEGACREDITS, 1)).victoryPoints(1).build());

        cards.add(new Card.Builder("Earth Office", 1)
                .tags(TagType.EARTH).description("Increase your MC production by 1 for each Earth tag you have.").effect((p, b, m) -> p.increaseProduction(ResourceType.MEGACREDITS, p.countTags(TagType.EARTH))).victoryPoints(1).build());

        cards.add(new Card.Builder("Orbital Construction", 15)
                .tags(TagType.SPACE, TagType.BUILDING).description("Increase your steel production by 1 and titanium production by 1.").effect((p, b, m) -> {p.increaseProduction(ResourceType.STEEL, 1);p.increaseProduction(ResourceType.TITANIUM, 1);}).victoryPoints(1).build());

        cards.add(new Card.Builder("Symbiotic Fungus", 4)
                .tags(TagType.PLANT).description("Requirement: You must have a Plant tag. Effect: Gain 1 plant.").requirement((p, b) -> p.countTags(TagType.PLANT) > 0).effect((p, b, m) -> p.addResource(ResourceType.PLANTS, 1)).build());

        cards.add(new Card.Builder("Nuclear Power", 10)
                .tags(TagType.ENERGY, TagType.EARTH).description("Decrease your MC production 2 steps and increase your energy production 3 steps.").effect((p, b, m) -> {p.increaseProduction(ResourceType.MEGACREDITS, -2);p.increaseProduction(ResourceType.ENERGY, 3);}).build());

        cards.add(new Card.Builder("Mangrove", 12)
                .tags(TagType.PLANT).description("Place a greenery tile and gain 3 plants.").effect((p, b, m) -> p.addResource(ResourceType.PLANTS, 3)).tileToPlace(TileType.GREENERY).victoryPoints(0).build());

        cards.add(new Card.Builder("Giant Ice Asteroid", 36)
                .tags(TagType.SPACE).description("Increase temperature by 1 step and place 1 ocean tile.").effect((p, b, m) -> {b.increaseTemperature();p.increaseTR(1);}).tileToPlace(TileType.OCEAN).build());

        cards.add(new Card.Builder("Aquifer Placement", 18)
                .tags(TagType.SPACE)
                .description("Place 1 ocean tile. You get 2 MC.")
                .effect((p, b, m) -> {
                    b.canPlaceOcean();
                    p.addMC(2);
                }).tileToPlace(TileType.OCEAN).build());

        return cards;
    }

    public static Card getCardByName(String name) {
        return allCards.stream()
                .filter(c -> c.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }
}