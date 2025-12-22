package hr.terraforming.mars.terraformingmars.manager;

import hr.terraforming.mars.terraformingmars.controller.game.SellPatentsController;
import hr.terraforming.mars.terraformingmars.controller.game.GameScreenController;
import hr.terraforming.mars.terraformingmars.enums.*;
import hr.terraforming.mars.terraformingmars.model.*;
import hr.terraforming.mars.terraformingmars.thread.SaveNewGameMoveThread;
import hr.terraforming.mars.terraformingmars.util.ScreenUtils;
import hr.terraforming.mars.terraformingmars.util.XmlUtils;
import javafx.application.Platform;
import javafx.stage.Window;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import java.util.List;
import java.util.function.Consumer;

@Slf4j
public class ActionManager {

    @Getter private final GameScreenController controller;
    @Getter private final GameFlowManager gameFlowManager;
    @Getter private final GameMoveManager gameMoveManager;
    private final ExecutionManager executionManager;

    public ActionManager(GameScreenController controller,
                         GameFlowManager gameFlowManager) {
        this.controller = controller;
        this.gameFlowManager = gameFlowManager;
        this.gameMoveManager = new GameMoveManager( this);
        this.executionManager = new ExecutionManager(controller, this, gameFlowManager);
    }

    private GameManager gm() {
        return controller.getGameManager();
    }

    public void processMove(GameMove move) {
        gameMoveManager.processMove(move);
    }

    public void recordAndSaveMove(GameMove move) {
        XmlUtils.appendGameMove(move);

        new Thread(new SaveNewGameMoveThread(move)).start();
        Platform.runLater(() -> controller.updateLastMoveLabel(move));

        controller.onLocalPlayerMove(move);
    }

    public void performAction() {
        gm().incrementActionsTaken();

        PlayerType playerType = ApplicationConfiguration.getInstance().getPlayerType();
        if (playerType == PlayerType.LOCAL || playerType == PlayerType.HOST) {
            controller.updateAllUI();
        }

        if (gm().getActionsTakenThisTurn() >= 2) {
            log.info("Player has taken 2 actions. Automatically passing turn.");
            if (gm().getCurrentPhase() == GamePhase.ACTIONS) {
                handlePassTurn();
            } else {
                log.info("Skipping auto-pass - phase: {}", gm().getCurrentPhase());
            }
        }
    }

    public void handlePassTurn() {
        executionManager.handlePassTurn();
    }

    public void handlePlayCard(Card card) {
        executionManager.handlePlayCard(card);
    }

    public void handleClaimMilestone(Milestone milestone) {
        executionManager.handleClaimMilestone(milestone);
    }

    public void handleStandardProject(StandardProject project) {
        executionManager.handleStandardProject(project);
    }

    public void handleConvertHeat() {
        executionManager.handleConvertHeat();
    }

    public void handleConvertPlants() {
        executionManager.handleConvertPlants();
    }

    public void openSellPatentsWindow() {
        Consumer<List<Card>> onSaleCompleteAction = soldCards -> {

            int count = soldCards.size();
            String patent = (count == 1) ? "patent" : "patents";
            String details = "sold: " + count + " " + patent + " for " + count + " MC";

            GameMove showModal = new GameMove(
                    gm().getCurrentPlayer().getName(),
                    ActionType.OPEN_SELL_PATENTS_MODAL,
                    details,
                    java.time.LocalDateTime.now()
            );
            recordAndSaveMove(showModal);

            GameMove move = new GameMove(
                    gm().getCurrentPlayer().getName(),
                    ActionType.SELL_PATENTS,
                    details,
                    java.time.LocalDateTime.now()
            );

            recordAndSaveMove(move);

            performAction();
        };

        Window owner = controller.getHexBoardPane().getScene().getWindow();

        ScreenUtils.showAsModal(
                owner,
                "SellPatents.fxml",
                "Sell Patents",
                0.5, 0.7,
                (SellPatentsController c) -> c.initData(gm().getCurrentPlayer(), onSaleCompleteAction)
        );
    }
}