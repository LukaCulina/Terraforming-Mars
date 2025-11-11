package hr.terraforming.mars.terraformingmars.factory;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import hr.terraforming.mars.terraformingmars.config.ResourceConfig;
import hr.terraforming.mars.terraformingmars.enums.TagType;
import hr.terraforming.mars.terraformingmars.enums.TileType;
import hr.terraforming.mars.terraformingmars.model.Card;
import hr.terraforming.mars.terraformingmars.model.CardData; // Potrebno kreirati ovu klasu/record
import hr.terraforming.mars.terraformingmars.model.GameBoard;
import hr.terraforming.mars.terraformingmars.model.Player;
import hr.terraforming.mars.terraformingmars.model.effects.Effect; // Potrebno kreirati
import hr.terraforming.mars.terraformingmars.model.effects.EffectInterpreter; // Potrebno kreirati
import hr.terraforming.mars.terraformingmars.model.requirements.RequirementInterpreter;
import lombok.Setter;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.BiPredicate;

public class CardFactory {

    private static final Map<String, Card> cardRegistry = new HashMap<>();
    @Setter
    private static ResourceConfig config;

    private CardFactory() {}

    public static void loadAllCards() {
        if (!cardRegistry.isEmpty()) {
            return;
        }

        String path = config.cardsPath();
        InputStream stream = CardFactory.class.getResourceAsStream(path);
        if (stream == null) {
            throw new IllegalStateException("Cannot find cards.json at path: " + path);
        }

        Gson gson = new Gson();
        Type cardListType = new TypeToken<ArrayList<CardData>>() {}.getType();
        List<CardData> allCardData = gson.fromJson(new InputStreamReader(stream), cardListType);

        for (CardData data : allCardData) {
            List<Effect> effects = EffectInterpreter.parseEffects(data.effects());

            BiPredicate<Player, GameBoard> requirement = RequirementInterpreter.parseRequirement(data.requirements());

            List<TagType> tagEnums = convertStringsToTagTypes(data.tags());

            TileType tileToPlace = null;
            if (data.tileToPlace() != null && !data.tileToPlace().isEmpty()) {
                tileToPlace = TileType.valueOf(data.tileToPlace());
            }

            Card card = new Card.Builder(data.name(), data.cost())
                    .description(data.description())
                    .tags(tagEnums.toArray(new TagType[0]))
                    .victoryPoints(data.victoryPoints())
                    .effects(effects)
                    .requirement(requirement)
                    .tileToPlace(tileToPlace)
                    .build();

            cardRegistry.put(card.getName(), card);
        }
    }

    public static List<Card> getAllCards() {
        return new ArrayList<>(cardRegistry.values());
    }

    public static Card getCardByName(String name) {
        return cardRegistry.get(name);
    }

    private static List<TagType> convertStringsToTagTypes(List<String> tagsAsStrings) {
        if (tagsAsStrings == null) {
            return Collections.emptyList();
        }
        return tagsAsStrings.stream()
                .map(String::toUpperCase)
                .map(TagType::valueOf)
                .toList();
    }
}
