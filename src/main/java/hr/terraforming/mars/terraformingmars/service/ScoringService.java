package hr.terraforming.mars.terraformingmars.service;

import hr.terraforming.mars.terraformingmars.model.Player;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
public class ScoringService {

    private ScoringService() {
        throw new IllegalStateException("Service class - use static methods");
    }

    public static List<Player> calculateFinalScores(List<Player> players) {
        for (Player player : players) {
            player.calculateTilePoints();
        }

        List<Player> rankedPlayers = new ArrayList<>(players);
        rankedPlayers.sort(
                Comparator.comparingInt(Player::getFinalScore).reversed()
                        .thenComparing(Player::getMC, Comparator.reverseOrder())
        );

        for (int i = 0; i < rankedPlayers.size(); i++) {
            Player player = rankedPlayers.get(i);
            log.info("#{} - {} with {} points (MC: {})",
                    i + 1, player.getName(), player.getFinalScore(), player.getMC());
        }

        return rankedPlayers;
    }
}