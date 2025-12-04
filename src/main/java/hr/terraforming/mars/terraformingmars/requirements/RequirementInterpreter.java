package hr.terraforming.mars.terraformingmars.requirements;

import hr.terraforming.mars.terraformingmars.enums.ResourceType;
import hr.terraforming.mars.terraformingmars.enums.TagType;
import hr.terraforming.mars.terraformingmars.model.GameBoard;
import hr.terraforming.mars.terraformingmars.model.Player;

import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;

public class RequirementInterpreter {

    private RequirementInterpreter() {
        throw new IllegalStateException("Utility class");
    }

    private static final String AMOUNT = "amount";

    public static BiPredicate<Player, GameBoard> parseRequirement(List<Map<String, Object>> reqDataList) {
        if (reqDataList == null || reqDataList.isEmpty()) {
            return (_, _) -> true;
        }

        BiPredicate<Player, GameBoard> finalRequirement = (_, _) -> true;

        for (Map<String, Object> data : reqDataList) {
            String type = (String) data.get("type");

            BiPredicate<Player, GameBoard> currentReq = switch (type) {
                case "minProduction" -> {
                    ResourceType resource = ResourceType.valueOf((String) data.get("resource"));
                    int amount = ((Double) data.get(AMOUNT)).intValue();
                    yield (p, _) -> p.getProduction(resource) >= amount;
                }

                case "minTags" -> {
                    TagType tag = TagType.valueOf((String) data.get("tag"));
                    int amount = ((Double) data.get(AMOUNT)).intValue();
                    yield (p, _) -> p.countTags(tag) >= amount;
                }

                case "minOceans" -> {
                    int amount = ((Double) data.get(AMOUNT)).intValue();
                    yield (_, gb) -> gb.getOceansPlaced() >= amount;
                }
                case "minOxygen" -> {
                    int amount = ((Double) data.get(AMOUNT)).intValue();
                    yield (_, gb) -> gb.getOxygenLevel() >= amount;
                }
                case "maxOxygen" -> {
                    int amount = ((Double) data.get(AMOUNT)).intValue();
                    yield (_, gb) -> gb.getOxygenLevel() <= amount;
                }
                case "maxTemperature" -> {
                    int amount = ((Double) data.get(AMOUNT)).intValue();
                    yield (_, gb) -> gb.getTemperature() <= amount;
                }

                default -> throw new IllegalArgumentException("Unknown requirement type: " + type);
            };

            finalRequirement = finalRequirement.and(currentReq);
        }

        return finalRequirement;
    }
}

