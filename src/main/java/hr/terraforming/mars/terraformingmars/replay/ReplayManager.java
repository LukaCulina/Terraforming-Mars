package hr.terraforming.mars.terraformingmars.replay;

import hr.terraforming.mars.terraformingmars.controller.game.GameScreenController;
import hr.terraforming.mars.terraformingmars.model.GameBoard;
import hr.terraforming.mars.terraformingmars.model.GameMove;
import hr.terraforming.mars.terraformingmars.model.Player;
import hr.terraforming.mars.terraformingmars.util.DialogUtils;
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

    private final GameScreenController controller;
    private final ReplayLoader loader;
    private final ReplayMoveExecutor actionHandler;
    private Timeline replayTimeline;

    public ReplayManager(GameScreenController controller) {
        this.controller = controller;
        this.loader = new ReplayLoader();
        this.actionHandler = new ReplayMoveExecutor(controller, loader);
    }

    public void prepareForReplay() {
        controller.getGameManager().getPlayers().forEach(Player::resetForNewGame);

        GameBoard newReplayBoard = new GameBoard();
        newReplayBoard.setOnGlobalParametersChanged(controller::refreshGameScreen);

        controller.setGameBoard(newReplayBoard);
        controller.getGameManager().resetForNewGame(newReplayBoard);

        if (controller.getGameScreenManager() != null) {
            controller.getGameScreenManager().updateHexBoardDrawer();
        }

        controller.setViewedPlayer(controller.getGameManager().getCurrentPlayer());
        controller.refreshGameScreen();
    }


    public void startReplay() {
        if (controller.getMoveHistoryTimeline() != null) {
            controller.getMoveHistoryTimeline().stop();
        }

        if (replayTimeline != null) {
            replayTimeline.stop();
        }

        List<GameMove> replayMoves = loader.loadMoves();

        if (replayMoves.isEmpty()) {
            log.warn("No game moves to replay");
            Platform.runLater(() ->
                    DialogUtils.showDialog(Alert.AlertType.WARNING, "Replay", "No game moves found to replay.")
            );
            return;
        }

        prepareForReplay();
        loader.setupInitialState(replayMoves, controller.getGameManager());
        controller.refreshGameScreen();

        AtomicInteger moveIndex = new AtomicInteger(0);

        replayTimeline = new Timeline(new KeyFrame(Duration.seconds(1), _ -> {
            if (moveIndex.get() < replayMoves.size()) {
                GameMove move = replayMoves.get(moveIndex.getAndIncrement());
                actionHandler.executeReplayMove(move);
                controller.refreshGameScreen();
            }
        }));

        replayTimeline.setCycleCount(replayMoves.size());
        replayTimeline.setOnFinished(_ -> onReplayFinished());
        replayTimeline.play();
    }

    private void onReplayFinished() {
        if (controller.getGameBoard().isFinalGeneration()) {
            List<Player> finalRankedPlayers = controller.getGameManager().calculateFinalScores();
            Platform.runLater(() -> ScreenNavigator.showGameOverScreen(finalRankedPlayers));
        } else {
            Platform.runLater(() -> {
                DialogUtils.showDialog(
                        Alert.AlertType.INFORMATION,
                        "Replay Over",
                        "The replay was played to the end, but the conditions for the end of the game were not met."
                );
                ScreenNavigator.showStartMenu();
            });
        }
        actionHandler.clearLastMoveLabel();
    }
}
