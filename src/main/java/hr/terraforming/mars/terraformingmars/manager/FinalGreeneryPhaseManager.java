package hr.terraforming.mars.terraformingmars.manager;

import hr.terraforming.mars.terraformingmars.controller.game.FinalGreeneryController;
import hr.terraforming.mars.terraformingmars.controller.game.TerraformingMarsController;
import hr.terraforming.mars.terraformingmars.enums.PlayerType;
import hr.terraforming.mars.terraformingmars.model.*;
import hr.terraforming.mars.terraformingmars.network.message.FinalGreeneryOfferMessage;
import hr.terraforming.mars.terraformingmars.network.message.GameOverMessage;
import hr.terraforming.mars.terraformingmars.util.ScreenLoader;
import hr.terraforming.mars.terraformingmars.view.ScreenNavigator;
import javafx.application.Platform;
import javafx.stage.Window;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class FinalGreeneryPhaseManager {

    private final GameManager gameManager;
    private final Window ownerWindow;
    private final Runnable onComplete;
    private int currentPlayerIndex = 0;
    private final TerraformingMarsController controller;

    public FinalGreeneryPhaseManager(GameManager gameManager, Window ownerWindow,
                                     TerraformingMarsController controller, Runnable onComplete) {
        this.gameManager = gameManager;
        this.ownerWindow = ownerWindow;
        this.controller = controller;
        this.onComplete = onComplete;
    }

    public void start() {
        this.currentPlayerIndex = 0;
        Platform.runLater(this::showScreenForNextPlayer);
    }

    public void showScreenForNextPlayer() {
        if (currentPlayerIndex >= gameManager.getPlayers().size()) {
            handleAllPlayersFinished();
            return;
        }

        handleCurrentPlayerTurn();
    }

    private void handleAllPlayersFinished() {
        log.info("🏁 All players finished Final Greenery!");

        PlayerType playerType = ApplicationConfiguration.getInstance().getPlayerType();

        if (playerType == PlayerType.HOST) {
            List<Player> rankedPlayers = gameManager.calculateFinalScores();

            var server = ApplicationConfiguration.getInstance().getGameServer();
            if (server != null) {
                log.info("HOST broadcasting final GameState");
                GameBoard gameBoard = gameManager.getGameBoard();
                server.broadcastGameState(new GameState(gameManager, gameBoard));

                log.info("HOST sending GameOverMessage to all clients");
                server.broadcastToAll(new GameOverMessage());
            }

            Platform.runLater(() -> ScreenNavigator.showGameOverScreen(rankedPlayers));
        } else if (playerType == PlayerType.LOCAL) {
            onComplete.run();
        }
    }

    private void handleCurrentPlayerTurn() {
        Player currentPlayer = gameManager.getPlayers().get(currentPlayerIndex);
        String myPlayerName = ApplicationConfiguration.getInstance().getMyPlayerName();
        PlayerType playerType = ApplicationConfiguration.getInstance().getPlayerType();

        log.info("Final Greenery turn for: {}", currentPlayer.getName());

        if (playerType == PlayerType.LOCAL) {
            openModalForPlayer(currentPlayer);
            return;
        }

        if (playerType != PlayerType.HOST) {
            return;
        }

        if (currentPlayer.getName().equals(myPlayerName)) {
            log.info("HOST opening modal for himself");
            openModalForPlayer(currentPlayer);
        } else {
            var server = ApplicationConfiguration.getInstance().getGameServer();
            if (server != null) {
                log.info("HOST sending FinalGreeneryOffer to {}", currentPlayer.getName());
                server.sendToPlayer(
                        currentPlayer.getName(),
                        new FinalGreeneryOfferMessage(currentPlayer.getName())
                );
            }
        }
    }

    private void openModalForPlayer(Player player) {
        ScreenLoader.showAsModal(
                ownerWindow,
                "FinalGreeneryScreen.fxml",
                "Final Greenery Conversion - " + player.getName(),
                0.4, 0.5,
                (FinalGreeneryController c) -> c.setupSinglePlayer(
                        player, gameManager, controller, this::finishForCurrentPlayer
                )
        );
    }

    void finishForCurrentPlayer() {
        if (currentPlayerIndex >= gameManager.getPlayers().size()) {
            log.warn("FinishForCurrentPlayer called but Final Greenery already complete");
            return;
        }

        Player currentPlayer = gameManager.getPlayers().get(currentPlayerIndex);
        log.info("{} finished Final Greenery placement", currentPlayer.getName());

        gameManager.advanceDraftPlayer();

        var config = ApplicationConfiguration.getInstance();
        if (config.getPlayerType() == PlayerType.HOST) {
            var broadcaster = config.getBroadcaster();
            if (broadcaster != null) {
                log.info("HOST broadcasting after {} finished", currentPlayer.getName());
                broadcaster.broadcast();
            }
        }

        currentPlayerIndex++;
        Platform.runLater(this::showScreenForNextPlayer);
    }
}