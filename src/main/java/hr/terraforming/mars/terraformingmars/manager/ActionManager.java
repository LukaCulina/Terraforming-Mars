package hr.terraforming.mars.terraformingmars.manager;

import hr.terraforming.mars.terraformingmars.controller.SellPatentsController;
import hr.terraforming.mars.terraformingmars.controller.TerraformingMarsController;
import hr.terraforming.mars.terraformingmars.enums.*;
import hr.terraforming.mars.terraformingmars.model.*;
import hr.terraforming.mars.terraformingmars.service.CostService;
import hr.terraforming.mars.terraformingmars.thread.SaveNewGameMoveThread;
import hr.terraforming.mars.terraformingmars.util.ScreenLoader;
import hr.terraforming.mars.terraformingmars.util.XmlUtils;
import javafx.application.Platform;
import javafx.stage.Window;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;

@Slf4j
public class ActionManager {

    @Getter
    private final TerraformingMarsController controller;
    @Getter
    private GameManager gameManager;
    @Getter
    private GameBoard gameBoard;
    @Getter
    private final GameFlowManager gameFlowManager;
    @Getter private final MoveManager moveManager;

    public ActionManager(TerraformingMarsController controller, GameManager gameManager, GameBoard gameBoard,
                         GameFlowManager gameFlowManager) {
        this.controller = controller;
        this.gameManager = gameManager;
        this.gameBoard = gameBoard;
        this.gameFlowManager = gameFlowManager;
        this.moveManager = new MoveManager(gameManager, gameBoard, this);
    }

    private boolean isLocalPlayerMove(Player player) {
        String myName = ApplicationConfiguration.getInstance().getMyPlayerName();
        return player.getName().equals(myName);
    }

    public void processMove(GameMove move) {
        moveManager.processMove(move);
    }

    public void updateState(GameManager newManager, GameBoard newBoard) {
        this.gameManager = newManager;
        this.gameBoard = newBoard;

        if (gameFlowManager != null) {
            gameFlowManager.updateState(newManager, newBoard);
        }
    }

    public void recordAndSaveMove(GameMove move) {
        XmlUtils.appendGameMove(move);

        new Thread(new SaveNewGameMoveThread(move)).start();
        Platform.runLater(() -> controller.updateLastMoveLabel(move));

        controller.onLocalPlayerMove(move);

    }

    public void performAction() {
        gameManager.incrementActionsTaken();
        controller.updateAllUI();

        if (gameManager.getActionsTakenThisTurn() >= 2) {
            log.info("Player has taken 2 actions. Automatically passing turn.");
            handlePassTurn();
        }
    }

    public void handlePassTurn() {
        String myName = hr.terraforming.mars.terraformingmars.model.ApplicationConfiguration.getInstance().getMyPlayerName();
        String currentTurnName = gameManager.getCurrentPlayer().getName();

        log.info("🛑 [ActionManager] handlePassTurn called. MyName='{}', CurrentTurn='{}'. Am I active? {}",
                myName, currentTurnName, currentTurnName.equals(myName));
        if (gameManager.getCurrentPhase() != GamePhase.ACTIONS) return;

        GameMove move = new GameMove(currentTurnName, ActionType.PASS_TURN, "Passed turn", LocalDateTime.now());

        if (gameManager.getActionsTakenThisTurn() < 2) {
            log.info("{} consciously passed the turn with {} actions taken.",
                    gameManager.getCurrentPlayer().getName(), gameManager.getActionsTakenThisTurn());
        } else {
            log.info("Turn for {} ended automatically after 2 actions.",
                    gameManager.getCurrentPlayer().getName());
        }

        boolean allPlayersPassed = gameManager.passTurn();

        recordAndSaveMove(move);

        if (allPlayersPassed) {
            PlayerType playerType = ApplicationConfiguration.getInstance().getPlayerType();
            log.info("🔍 All players passed! PlayerType={}, isHOST={}, isLOCAL={}, isCLIENT={}",
                    playerType,
                    playerType == PlayerType.HOST,
                    playerType == PlayerType.LOCAL,
                    playerType == PlayerType.CLIENT);
            if (playerType == PlayerType.HOST || playerType == PlayerType.LOCAL) {
                gameFlowManager.handleEndOfActionPhase();
            } else {
                log.info("CLIENT: All players passed, waiting for server to change phase.");
            }
        } else {
            controller.setViewedPlayer(gameManager.getCurrentPlayer());
            controller.updateAllUI();
        }
    }

    public void handlePlayCard(Card card) {
        Player currentPlayer = gameManager.getCurrentPlayer();
        if (!currentPlayer.canPlayCard(card)) {
            log.warn("Player {} cannot play card '{}'. Requirements not met or insufficient funds.",
                    currentPlayer.getName(), card.getName());
            return;
        }

        GameMove move = new GameMove(currentPlayer.getName(), ActionType.PLAY_CARD, card.getName(), LocalDateTime.now());

        if (card.getTileToPlace() != null) {
            if (isLocalPlayerMove(currentPlayer)) {
                controller.getPlacementCoordinator().enterPlacementModeForCard(card, move);
            } else {
                currentPlayer.playCard(card, gameManager);
                log.info("Network: {} played card {} (tile placement pending)", currentPlayer.getName(), card.getName());
            }
        } else {
            currentPlayer.playCard(card, gameManager);
            performAction();
            recordAndSaveMove(move);
        }
    }

    public void handleClaimMilestone(Milestone milestone) {
        Player currentPlayer = gameManager.getCurrentPlayer();
        final int MILESTONE_COST = 8;

        if (gameBoard.claimMilestone(milestone, currentPlayer)) {
            currentPlayer.spendMC(MILESTONE_COST);
            performAction();
            GameMove move = new GameMove(currentPlayer.getName(), ActionType.CLAIM_MILESTONE, milestone.name(), LocalDateTime.now());
            recordAndSaveMove(move);
        } else {
            log.warn("Failed attempt by {} to claim milestone '{}'.", currentPlayer.getName(), milestone.getName());
        }
    }

    public void handleStandardProject(StandardProject project) {
        Player currentPlayer = gameManager.getCurrentPlayer();
        int finalCost = CostService.getFinalProjectCost(project, currentPlayer);

        if (currentPlayer.getMC() < finalCost) {
            log.warn("{} failed to use standard project '{}': insufficient MC (has {}, needs {}).",
                    currentPlayer.getName(), project.getName(), currentPlayer.getMC(), project.getCost());
            return;
        }

        GameMove move = new GameMove(gameManager.getCurrentPlayer().getName(), ActionType.USE_STANDARD_PROJECT, project.name(), LocalDateTime.now());

        if (project.requiresTilePlacement()) {
            if (isLocalPlayerMove(currentPlayer)) {
                controller.getPlacementCoordinator().enterPlacementModeForProject(project, move);
            } else {
                currentPlayer.spendMC(finalCost);
                log.info("Network: {} used standard project {} (tile placement pending)", currentPlayer.getName(), project.getName());
            }
        } else {
            if (project == StandardProject.SELL_PATENTS) {
                if (currentPlayer.getHand().isEmpty()) {
                    log.warn("{} tried to sell patents but has no cards in hand.", currentPlayer.getName());
                    return;
                }
                if (isLocalPlayerMove(currentPlayer)) {
                    openSellPatentsWindow();
                }
            } else {
                currentPlayer.spendMC(finalCost);
                project.execute(currentPlayer, gameBoard);
                performAction();
                recordAndSaveMove(move);
            }
        }
    }

    public void handleConvertHeat() {
        Player currentPlayer = gameManager.getCurrentPlayer();

        if (currentPlayer.resourceProperty(ResourceType.HEAT).get() < 8) {
            log.warn("{} failed to convert heat: insufficient resources.", currentPlayer.getName());
            return;
        }

        log.info("{} is converting 8 heat.", currentPlayer.getName());

        currentPlayer.resourceProperty(ResourceType.HEAT).set(currentPlayer.resourceProperty(ResourceType.HEAT).get() - 8);
        gameBoard.increaseTemperature();
        currentPlayer.increaseTR(1);
        performAction();

        GameMove move = new GameMove(currentPlayer.getName(), ActionType.CONVERT_HEAT, "Raise temperature", LocalDateTime.now());
        recordAndSaveMove(move);
    }

    public void handleConvertPlants() {
        Player currentPlayer = gameManager.getCurrentPlayer();
        int requiredPlants = currentPlayer.getGreeneryCost();

        if (currentPlayer.resourceProperty(ResourceType.PLANTS).get() < requiredPlants) {
            log.warn("{} failed to convert plants: insufficient resources.", currentPlayer.getName());
            return;
        }

        log.info("{} is initiating greenery conversion.", currentPlayer.getName());

        GameMove convertPlantsMove = new GameMove(
                currentPlayer.getName(),
                ActionType.CONVERT_PLANTS,
                "Convert " + requiredPlants + " plants to greenery",
                LocalDateTime.now()
        );

        if (isLocalPlayerMove(currentPlayer)) {
            controller.getPlacementCoordinator().enterPlacementModeForPlant(convertPlantsMove);
        } else {
            currentPlayer.spendPlantsForGreenery();
            log.info("Network: {} converting plants (waiting for PLACE_TILE)", currentPlayer.getName());
        }    }

    public void openSellPatentsWindow() {
        Consumer<List<Card>> onSaleCompleteAction = soldCards -> {

            String details = soldCards.stream().map(Card::getName).reduce((a,b) -> a + "," + b).orElse("");
            GameMove showModal = new GameMove(
                    gameManager.getCurrentPlayer().getName(),
                    ActionType.OPEN_SELL_PATENTS_MODAL,
                    details,
                    java.time.LocalDateTime.now()
            );
            recordAndSaveMove(showModal);

            GameMove move = new GameMove(
                    gameManager.getCurrentPlayer().getName(),
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
                (SellPatentsController c) -> c.initData(gameManager.getCurrentPlayer(), onSaleCompleteAction)
        );
    }
}