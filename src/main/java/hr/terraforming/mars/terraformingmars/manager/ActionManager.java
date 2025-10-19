package hr.terraforming.mars.terraformingmars.manager;

import hr.terraforming.mars.terraformingmars.controller.TerraformingMarsController;
import hr.terraforming.mars.terraformingmars.enums.*;
import hr.terraforming.mars.terraformingmars.model.*;
import hr.terraforming.mars.terraformingmars.service.CostService;
import hr.terraforming.mars.terraformingmars.thread.SaveNewGameMoveThread;
import hr.terraforming.mars.terraformingmars.util.XmlUtils;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;

public record ActionManager(TerraformingMarsController controller, GameManager gameManager, GameBoard gameBoard,
                            PlacementManager placementManager, GameFlowManager gameFlowManager) {

    private static final Logger logger = LoggerFactory.getLogger(ActionManager.class);

    public void recordAndSaveMove(GameMove move) {
        XmlUtils.appendGameMove(move);

        new Thread(new SaveNewGameMoveThread(move)).start();
        Platform.runLater(() -> controller.updateLastMoveLabel(move));
    }

    public void performAction() {
        gameManager.incrementActionsTaken();

        controller.updateAllUI();

        if (gameManager.getActionsTakenThisTurn() >= 2) {
            logger.info("Player has taken 2 actions. Automatically passing turn.");

            handlePassTurn();
        }
    }

    public void handlePassTurn() {
        if (gameManager.getCurrentPhase() != GamePhase.ACTIONS) return;

        if (gameManager.getActionsTakenThisTurn() < 2) {
            GameMove move = new GameMove(gameManager.getCurrentPlayer().getName(), ActionType.PASS_TURN, "Passed turn", LocalDateTime.now());
            recordAndSaveMove(move);

            logger.info("{} consciously passed the turn with {} actions taken.",
                    gameManager.getCurrentPlayer().getName(), gameManager.getActionsTakenThisTurn());
        } else {
            logger.info("Turn for {} ended automatically after 2 actions.",
                    gameManager.getCurrentPlayer().getName());
        }

        boolean allPlayersPassed = gameManager.passTurn();

        if (allPlayersPassed) {
            gameFlowManager.handleEndOfActionPhase();
        } else {
            controller.setViewedPlayer(gameManager.getCurrentPlayer());
            controller.updateAllUI();
        }
    }

    public void handlePlayCard(Card card) {
        Player currentPlayer = gameManager.getCurrentPlayer();
        if (!currentPlayer.canPlayCard(card)) {
            logger.warn("Player {} cannot play card '{}'. Requirements not met or insufficient funds.",
                    currentPlayer.getName(), card.getName());
            return;
        }

        GameMove move = new GameMove(currentPlayer.getName(), ActionType.PLAY_CARD, card.getName(), LocalDateTime.now());

        if (card.getTileToPlace() != null) {
            placementManager.enterPlacementModeForCard(card, move);
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
            logger.warn("Failed attempt by {} to claim milestone '{}'.", currentPlayer.getName(), milestone.getName());
        }
    }

    public void handleStandardProject(StandardProject project) {
        Player currentPlayer = gameManager.getCurrentPlayer();

        int finalCost = CostService.getFinalProjectCost(project, currentPlayer);

        if (currentPlayer.getMC() < finalCost) {
            logger.warn("{} failed to use standard project '{}': insufficient MC (has {}, needs {}).",
                    currentPlayer.getName(), project.getName(), currentPlayer.getMC(), project.getCost());
            return;
        }

        GameMove move = new GameMove(gameManager.getCurrentPlayer().getName(), ActionType.USE_STANDARD_PROJECT, project.name(), LocalDateTime.now());

        if (project.requiresTilePlacement()) {
            placementManager.enterPlacementModeForProject(project, move);
        } else {
            if (project == StandardProject.SELL_PATENTS) {
                if (currentPlayer.getHand().isEmpty()) {
                    logger.warn("{} tried to sell patents but has no cards in hand.", currentPlayer.getName());
                    return;
                }
                controller.openSellPatentsWindow();
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
            logger.warn("{} failed to convert heat: insufficient resources.", currentPlayer.getName());
            return;
        }

        logger.info("{} is converting 8 heat.", currentPlayer.getName());

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
            logger.warn("{} failed to convert plants: insufficient resources.", currentPlayer.getName());
            return;
        }

        logger.info("{} is initiating greenery conversion.", currentPlayer.getName());

        GameMove convertPlantsMove = new GameMove(
                currentPlayer.getName(),
                ActionType.CONVERT_PLANTS,
                "Convert " + requiredPlants + " plants to greenery",
                LocalDateTime.now()
        );

        placementManager.enterPlacementModeForPlant(convertPlantsMove);
    }
}
