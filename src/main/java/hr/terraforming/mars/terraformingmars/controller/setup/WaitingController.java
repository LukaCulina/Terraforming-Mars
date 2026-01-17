package hr.terraforming.mars.terraformingmars.controller.setup;

import hr.terraforming.mars.terraformingmars.model.GameManager;
import hr.terraforming.mars.terraformingmars.view.ScreenNavigator;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.util.Duration;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Slf4j
public class WaitingController {

    @FXML
    private Label playerCountLabel;

    @FXML
    private Label ipLabel;

    private GameManager gameManager;
    private int expectedPlayerCount;
    private Timeline pollingTimeline;

    public void setup(GameManager gameManager, int expectedPlayerCount) {
        this.gameManager = gameManager;
        this.expectedPlayerCount = expectedPlayerCount;

        displayServerIp();
        startPolling();
    }

    private void displayServerIp() {
        try {
            InetAddress localHost = InetAddress.getLocalHost();
            String ip = localHost.getHostAddress();

            ipLabel.setText("Server Address: " + ip);
            log.info("Server running on {}", ip);
        } catch (UnknownHostException e) {
            ipLabel.setText("Share this: localhost");
            log.warn("Could not determine local IP, using localhost", e);
        }
    }

    private void startPolling() {
        if (pollingTimeline != null) {
            pollingTimeline.stop();
        }

        pollingTimeline = new Timeline(new KeyFrame(Duration.millis(500), _ -> checkIfAllPlayersJoined()));

        pollingTimeline.setCycleCount(Animation.INDEFINITE);
        pollingTimeline.play();
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
            log.info("All {} players have joined!", expectedPlayerCount);
            cleanup();
            ScreenNavigator.showChooseCorporationScreen(gameManager);
        }
    }

    public void cleanup() {
        if (pollingTimeline != null) {
            pollingTimeline.stop();

            log.info("Polling timeline stopped");
        }
    }
}