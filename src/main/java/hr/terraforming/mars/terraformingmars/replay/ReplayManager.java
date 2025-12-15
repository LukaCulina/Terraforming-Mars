package hr.terraforming.mars.terraformingmars.replay;

import hr.terraforming.mars.terraformingmars.controller.game.TerraformingMarsController;
import hr.terraforming.mars.terraformingmars.model.GameBoard;
import hr.terraforming.mars.terraformingmars.model.GameMove;
import hr.terraforming.mars.terraformingmars.model.Player;
import hr.terraforming.mars.terraformingmars.view.ScreenNavigator;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.util.Duration;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class ReplayManager {

    private final TerraformingMarsController controller;
    private final ReplayLoader loader;
    private final ReplayActionHandler actionHandler;
    private Timeline replayTimeline;

    public ReplayManager(TerraformingMarsController controller) {
        this.controller = controller;
        this.loader = new ReplayLoader();
        this.actionHandler = new ReplayActionHandler(controller, loader);
    }

    public void prepareForReplay() {
        controller.getGameManager().getPlayers().forEach(Player::resetForNewGame);

        GameBoard newReplayBoard = new GameBoard();
        newReplayBoard.setOnGlobalParametersChanged(controller::updateAllUI);

        controller.setGameBoard(newReplayBoard);
        controller.getGameManager().resetForNewGame(newReplayBoard);

        if (controller.getUiManager() != null) {
            controller.getUiManager().updateHexBoardDrawer();
        }

        controller.setViewedPlayer(controller.getGameManager().getCurrentPlayer());
        controller.updateAllUI();
    }


    public void startReplay() {
        if (controller.getMoveHistoryTimeline() != null) {
            controller.getMoveHistoryTimeline().stop();
        }

        if (replayTimeline != null) {
            replayTimeline.stop();
        }

        List<GameMove> movesToReplay = loader.loadMoves();
        if (movesToReplay.isEmpty()) {
            actionHandler.showNoMovesToReplayAlert();
            return;
        }

        prepareForReplay();
        loader.setupInitialState(movesToReplay, controller.getGameManager());
        controller.updateAllUI();

        AtomicInteger moveIndex = new AtomicInteger(0);

        replayTimeline = new Timeline(new KeyFrame(Duration.seconds(1), _ -> {
            if (moveIndex.get() < movesToReplay.size()) {
                GameMove move = movesToReplay.get(moveIndex.getAndIncrement());
                actionHandler.executeReplayMove(move);
                controller.updateAllUI();
            }
        }));

        replayTimeline.setCycleCount(movesToReplay.size());
        replayTimeline.setOnFinished(_ -> onReplayFinished());
        replayTimeline.play();
    }

    private void onReplayFinished() {
        if (controller.getGameBoard().isFinalGeneration()) {
            List<Player> finalRankedPlayers = controller.getGameManager().calculateFinalScores();
            actionHandler.showGameOverScreen(finalRankedPlayers);
        } else {
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Replay Over");
                alert.setHeaderText(null);
                alert.setContentText("The replay was played to the end, but the conditions for the end of the game were not met.");
                alert.showAndWait();
                ScreenNavigator.showStartMenu();
            });
        }
        actionHandler.clearLastMoveLabel();
    }
}
