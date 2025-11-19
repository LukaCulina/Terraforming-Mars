package hr.terraforming.mars.terraformingmars.util;

import hr.terraforming.mars.terraformingmars.enums.ResourceType;
import hr.terraforming.mars.terraformingmars.model.Player;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
public class GamePhaseUtils {

    private GamePhaseUtils() {
        throw new IllegalStateException("Utility Class");
    }

    public static void executeProduction(List<Player> players) {
        log.info("Production Phase Started");
        for (Player p : players) {
            int income = p.getTR() + p.productionProperty(ResourceType.MEGACREDITS).get();
            p.addMC(income);

            int energy = p.resourceProperty(ResourceType.ENERGY).get();
            p.addResource(ResourceType.HEAT, energy);
            p.resourceProperty(ResourceType.ENERGY).set(0);

            p.getProductionMap().forEach((type, amount) -> {
                if (type != ResourceType.MEGACREDITS) {
                    p.addResource(type, amount.get());
                }
            });
        }
        log.info("Production Finished");
    }

    public static List<Player> calculateFinalScores(List<Player> players) {
        log.info("=== FINAL SCORING ===");
        for (Player p : players) {
            p.calculateTilePoints();
            log.info("{} - Final Score: {}", p.getName(), p.getFinalScore());
        }

        List<Player> rankedPlayers = new ArrayList<>(players);
        rankedPlayers.sort(
                Comparator.comparingInt(Player::getFinalScore).reversed()
                        .thenComparing(Player::getMC, Comparator.reverseOrder())
        );
        return rankedPlayers;
    }
}
