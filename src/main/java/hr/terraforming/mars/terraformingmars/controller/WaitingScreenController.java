package hr.terraforming.mars.terraformingmars.controller;

import hr.terraforming.mars.terraformingmars.model.GameManager;
import hr.terraforming.mars.terraformingmars.view.ScreenNavigator;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.util.Duration;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WaitingScreenController {
    @FXML
    private Label statusLabel;

    @FXML
    private Label playerCountLabel;
    private GameManager gameManager;
    private int expectedPlayerCount;
    private Timeline pollingTimeline;

    public void setup(GameManager gameManager, int expectedPlayerCount) {
        this.gameManager = gameManager;
        this.expectedPlayerCount = expectedPlayerCount;

        updateStatus();

        startPolling();
    }

    private void startPolling() {
        if (pollingTimeline != null) {
            pollingTimeline.stop();
        }

        pollingTimeline = new Timeline(new KeyFrame(Duration.millis(500), _ -> checkIfAllPlayersJoined()));

        pollingTimeline.setCycleCount(Animation.INDEFINITE);
        pollingTimeline.play();
    }

    public void cleanup() {
        if (pollingTimeline != null) {
            pollingTimeline.stop();
            log.info("Polling timeline stopped");
        }
    }

    private void checkIfAllPlayersJoined() {
        int joinedCount = 0;
        for (var player : gameManager.getPlayers()) {
            if (!player.getName().startsWith("Player ")) {
                joinedCount++;
            }
        }

        playerCountLabel.setText(joinedCount + " / " + expectedPlayerCount + " players connected");

        if (joinedCount >= expectedPlayerCount) {
            log.info("✅ All {} players have joined!", expectedPlayerCount);

            cleanup();

            ScreenNavigator.showChooseCorporationScreen(gameManager);
        }
    }

    private void updateStatus() {
        statusLabel.setText("Waiting for all players to join...");
    }
}