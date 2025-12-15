package hr.terraforming.mars.terraformingmars.manager;

import hr.terraforming.mars.terraformingmars.controller.game.SellPatentsController;
import hr.terraforming.mars.terraformingmars.controller.game.TerraformingMarsController;
import hr.terraforming.mars.terraformingmars.enums.*;
import hr.terraforming.mars.terraformingmars.model.*;
import hr.terraforming.mars.terraformingmars.thread.SaveNewGameMoveThread;
import hr.terraforming.mars.terraformingmars.util.ScreenLoader;
import hr.terraforming.mars.terraformingmars.util.XmlUtils;
import javafx.application.Platform;
import javafx.stage.Window;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import java.util.List;
import java.util.function.Consumer;

@Slf4j
public class ActionManager {

    @Getter private final TerraformingMarsController controller;
    @Getter private final GameFlowManager gameFlowManager;
    @Getter private final MoveManager moveManager;
    private final ActionExecutor actionExecutor;

    public ActionManager(TerraformingMarsController controller,
                         GameFlowManager gameFlowManager) {
        this.controller = controller;
        this.gameFlowManager = gameFlowManager;
        this.moveManager = new MoveManager( this);
        this.actionExecutor = new ActionExecutor(controller, this, gameFlowManager);
    }

    private GameManager gm() {
        return controller.getGameManager();
    }

    public void processMove(GameMove move) {
        moveManager.processMove(move);
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
            handlePassTurn();
        }
    }

    public void handlePassTurn() {
        actionExecutor.handlePassTurn();
    }

    public void handlePlayCard(Card card) {
        actionExecutor.handlePlayCard(card);
    }

    public void handleClaimMilestone(Milestone milestone) {
        actionExecutor.handleClaimMilestone(milestone);
    }

    public void handleStandardProject(StandardProject project) {
        actionExecutor.handleStandardProject(project);
    }

    public void handleConvertHeat() {
        actionExecutor.handleConvertHeat();
    }

    public void handleConvertPlants() {
        actionExecutor.handleConvertPlants();
    }

    public void openSellPatentsWindow() {
        Consumer<List<Card>> onSaleCompleteAction = soldCards -> {

            int count = soldCards.size();
            String patent = (count == 1) ? "patent" : "patents";
            String details = "Sold: " + count + " " + patent + " for " + count + " MC";

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

        ScreenLoader.showAsModal(
                owner,
                "SellPatents.fxml",
                "Sell Patents",
                0.5, 0.7,
                (SellPatentsController c) -> c.initData(gm().getCurrentPlayer(), onSaleCompleteAction)
        );
    }
}