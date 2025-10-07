package hr.terraforming.mars.terraformingmars.factory;

import hr.terraforming.mars.terraformingmars.model.Corporation;
import hr.terraforming.mars.terraformingmars.enums.ResourceType;

import java.util.*;

public class CorporationFactory {

    private CorporationFactory() {
        throw new IllegalStateException("Utility class");
    }

    public static List<Corporation> getAllCorporations() {
        List<Corporation> corporations = new ArrayList<>();

        corporations.add(new Corporation(
                "Credicor",
                57,
                Map.of(),
                Map.of(),
                "You pay 1 MC less for every card you play."
        ));

        corporations.add(new Corporation(
                "Helion",
                42,
                Map.of(),
                Map.of(ResourceType.HEAT, 3),
                "Each time you increase your Energy production, gain 2 MC."
        ));

        corporations.add(new Corporation(
                "Mining Guild",
                30,
                Map.of(ResourceType.STEEL, 5),
                Map.of(ResourceType.STEEL, 1),
                "You pay 2 MC less for cards with a BUILDING tag."
        ));

        corporations.add(new Corporation(
                "Phobolog",
                23,
                Map.of(ResourceType.TITANIUM, 10),
                Map.of(),
                "You pay 4 MC less for cards with a SPACE tag."
        ));

        corporations.add(new Corporation(
                "Ecoline",
                36,
                Map.of(ResourceType.PLANTS, 3),
                Map.of(ResourceType.PLANTS, 2),
                "You may always pay 7 plants (instead of 8) to place greenery."
        ));

        corporations.add(new Corporation(
                "Interplanetary Cinematics",
                30,
                Map.of(ResourceType.STEEL, 20),
                Map.of(),
                "Gain 2 MC each time you play a card with a SPACE tag."
        ));

        corporations.add(new Corporation(
                "Inventrix",
                45,
                Map.of(),
                Map.of(),
                "You pay 2 MC less for cards with a SCIENCE tag."
        ));

        corporations.add(new Corporation(
                "Tharsis Republic",
                40,
                Map.of(),
                Map.of(),
                "You pay 4 MC less for the Standard Project: City."
        ));

        corporations.add(new Corporation(
                "Thorgate",
                48,
                Map.of(),
                Map.of(ResourceType.ENERGY, 1),
                "You pay 3 MC less for cards with an ENERGY tag and for the Standard Project: Power Plant."
        ));

        corporations.add(new Corporation(
                "United Nations Mars Initiative",
                40,
                Map.of(),
                Map.of(),
                "Each time you increase your TR, gain 3 MC."
        ));

        corporations.add(new Corporation(
                "Teractor",
                60,
                Map.of(),
                Map.of(),
                "When playing a card with an EARTH tag, you pay 3 MC less."
        ));

        corporations.add(new Corporation(
                "Saturn Systems",
                42,
                Map.of(),
                Map.of(ResourceType.TITANIUM, 1),
                "Each time you play a card with a JOVIAN tag, increase your MC production by 1."
        ));

        return corporations;
    }
}