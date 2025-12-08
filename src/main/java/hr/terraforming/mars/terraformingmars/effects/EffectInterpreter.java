package hr.terraforming.mars.terraformingmars.effects;

import hr.terraforming.mars.terraformingmars.enums.ResourceType;
import hr.terraforming.mars.terraformingmars.enums.TagType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class EffectInterpreter {

    private EffectInterpreter() { throw new IllegalStateException("Utility class"); }

    private static final String RESOURCE = "resource";
    private static final String AMOUNT = "amount";

    public static List<Effect> parseEffects(List<Map<String, Object>> effectDataList) {
        if (effectDataList == null) {
            return Collections.emptyList();
        }

        List<Effect> effects = new ArrayList<>();
        for (Map<String, Object> data : effectDataList) {
            String type = (String) data.get("type");

            Effect effect = switch (type) {
                case "addResource" -> {
                    ResourceType resource = ResourceType.valueOf((String) data.get(RESOURCE));
                    int amount = ((Double) data.get(AMOUNT)).intValue();
                    yield (player, _) -> {
                        if (resource == ResourceType.MEGA_CREDITS) {
                            player.addMC(amount);
                        } else {
                            player.addResource(resource, amount);
                        }
                    };
                }
                case "increaseProduction" -> {
                    ResourceType resource = ResourceType.valueOf((String) data.get(RESOURCE));
                    int amount = ((Double) data.get(AMOUNT)).intValue();
                    yield (player, _) -> player.increaseProduction(resource, amount);
                }
                case "increaseProductionPerTag" -> {
                    ResourceType resource = ResourceType.valueOf((String) data.get(RESOURCE));
                    TagType tag = TagType.valueOf((String) data.get("tag"));
                    yield (player, _) -> player.increaseProduction(resource, player.countTags(tag));
                }

                case "gainMcPerTag" -> {
                    TagType tag = TagType.valueOf((String) data.get("tag"));
                    Number multiplierNum = (Number) data.get("multiplier");
                    double multiplier = multiplierNum.doubleValue();

                    boolean includeSelf = (boolean) data.getOrDefault("includeSelf", false);

                    yield (player, _) -> {
                        int tagCount = player.countTags(tag);
                        if (includeSelf) {
                            tagCount++;
                        }
                        player.addMC((int) Math.floor(tagCount * multiplier));
                    };
                }
                case "gainMcPerOpponent" -> {
                    int amount = ((Double) data.get(AMOUNT)).intValue();
                    yield (player, gm) -> player.addMC(amount * (gm.getPlayers().size() - 1));
                }

                case "drawCards" -> {
                    int amount = ((Double) data.get(AMOUNT)).intValue();
                    yield (player, gm) -> player.addCardsToHand(gm.drawCards(amount));
                }

                case "increaseTemperature" -> {
                    int amount = ((Double) data.getOrDefault(AMOUNT, 1.0)).intValue();
                    yield (_, gm) -> {
                        for (int i = 0; i < amount; i++) {
                            gm.getGameBoard().increaseTemperature();
                        }
                    };
                }
                case "increaseOxygen" -> {
                    int amount = ((Double) data.getOrDefault(AMOUNT, 1.0)).intValue();
                    yield (_, gm) -> {
                        for (int i = 0; i < amount; i++) {
                            gm.getGameBoard().increaseOxygen();
                        }
                    };
                }
                case "increaseTR" -> {
                    int amount = ((Double) data.get(AMOUNT)).intValue();
                    yield (player, _) -> player.increaseTR(amount);
                }
                case "canPlaceOcean" -> (_, gm) -> gm.getGameBoard().canPlaceOcean();

                default -> throw new IllegalArgumentException("Unknown effect type: " + type);
            };
            effects.add(effect);
        }
        return effects;
    }
}
