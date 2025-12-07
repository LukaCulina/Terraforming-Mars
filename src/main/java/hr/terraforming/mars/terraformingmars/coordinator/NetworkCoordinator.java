package hr.terraforming.mars.terraformingmars.coordinator;

import hr.terraforming.mars.terraformingmars.controller.game.TerraformingMarsController;
import hr.terraforming.mars.terraformingmars.enums.GamePhase;
import hr.terraforming.mars.terraformingmars.enums.PlayerType;
import hr.terraforming.mars.terraformingmars.model.*;
import hr.terraforming.mars.terraformingmars.network.*;
import javafx.application.Platform;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public record NetworkCoordinator(TerraformingMarsController controller) {

    public void handleNetworkUpdate(GameState state) {
        if (!validateGameState(state)) {
            return;
        }

        updateLocalState(state);

        Platform.runLater(() -> {
            updateViewedPlayer();
            updateGameControls();
            controller.updateAllUI();
            updatePlayerHighlight();
        });
    }

    public void broadcastMove(GameMove move) {
        PlayerType playerType = ApplicationConfiguration.getInstance().getPlayerType();

        if (playerType == PlayerType.HOST) {
            NetworkBroadcaster broadcaster = ApplicationConfiguration.getInstance().getBroadcaster();
            if (broadcaster != null) {
                broadcaster.broadcast();
                log.debug("📡 HOST broadcasted game state");
            }
        } else if (playerType == PlayerType.CLIENT) {
            GameClientThread client = ApplicationConfiguration.getInstance().getGameClient();
            if (client != null) {
                client.sendMove(move);
                log.debug("📤 CLIENT sent move: {}", move.actionType());
            }
        }
    }

    public void setupClientListeners() {
        PlayerType playerType = ApplicationConfiguration.getInstance().getPlayerType();

        if (playerType == PlayerType.CLIENT) {
            controller.setGameControlsEnabled(false);

            GameClientThread client = ApplicationConfiguration.getInstance().getGameClient();
            if (client != null) {
                client.addGameStateListener(controller::updateFromNetwork);
            }
        }
    }

    private boolean validateGameState(GameState state) {
        if (state == null) {
            log.error("❌ Received null GameState!");
            return false;
        }
        if (state.gameManager() == null || state.gameBoard() == null) {
            log.error("❌ Incomplete GameState! Manager={}, Board={}",
                    state.gameManager(), state.gameBoard());
            return false;
        }
        return true;
    }

    private void updateLocalState(GameState state) {
        controller.setGameManager(state.gameManager());
        controller.setGameBoard(state.gameBoard());

        controller.getGameManager().relink(controller.getGameBoard());

        if (controller.getUiManager() != null) {
            controller.getUiManager().updateGameState(
                    controller.getGameManager(),
                    controller.getGameBoard()
            );
        }
        if (controller.getActionManager() != null) {
            controller.getActionManager().updateState(
                    controller.getGameManager(),
                    controller.getGameBoard()
            );
        }
    }

    private void updateViewedPlayer() {
        String myPlayerName = ApplicationConfiguration.getInstance().getMyPlayerName();

        if (myPlayerName != null) {
            Player myPlayer = controller.getGameManager().getPlayerByName(myPlayerName);
            if (myPlayer != null) {
                controller.setViewedPlayer(myPlayer);
                return;
            }
        }
        controller.setViewedPlayer(controller.getGameManager().getCurrentPlayer());
    }

    private void updateGameControls() {
        String myPlayerName = ApplicationConfiguration.getInstance().getMyPlayerName();
        String currentPlayerName = controller.getGameManager().getCurrentPlayer().getName();

        boolean isMyTurn = currentPlayerName.equals(myPlayerName);
        boolean isActionPhase = controller.getGameManager().getCurrentPhase() == GamePhase.ACTIONS;

        if (isMyTurn && isActionPhase) {
            controller.setGameControlsEnabled(true);
            log.debug("✅ Controls ENABLED for {}", myPlayerName);
        } else {
            controller.setGameControlsEnabled(false);
            log.debug("🚫 Controls DISABLED (MyTurn: {}, ActionPhase: {})", isMyTurn, isActionPhase);
        }
    }

    private void updatePlayerHighlight() {
        controller.updatePlayerHighlightForCurrentPlayer();
    }
}
