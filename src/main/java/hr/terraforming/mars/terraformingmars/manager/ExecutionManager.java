package hr.terraforming.mars.terraformingmars.manager;

import hr.terraforming.mars.terraformingmars.controller.game.GameScreenController;
import hr.terraforming.mars.terraformingmars.enums.*;
import hr.terraforming.mars.terraformingmars.model.*;
import hr.terraforming.mars.terraformingmars.service.CostService;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

@Slf4j
public class ExecutionManager {

    private final GameScreenController controller;
    private final ActionManager actionManager;
    private final GameFlowManager gameFlowManager;

    public ExecutionManager(GameScreenController controller, ActionManager actionManager, GameFlowManager gameFlowManager) {
        this.controller = controller;
        this.actionManager = actionManager;
        this.gameFlowManager = gameFlowManager;
    }

    private GameManager gm() {
        return controller.getGameManager();
    }

    private GameBoard board() {
        return controller.getGameBoard();
    }

    private boolean isLocalPlayerMove(Player player) {
        if (ApplicationConfiguration.getInstance().getPlayerType() == PlayerType.LOCAL) {
            return true;
        }
        String myName = ApplicationConfiguration.getInstance().getMyPlayerName();
        return player.getName().equals(myName);
    }

    public void handlePassTurn() {
        if (gm().getCurrentPhase() != GamePhase.ACTIONS) return;

        String currentTurnName = gm().getCurrentPlayer().getName();
        GameMove move = new GameMove(currentTurnName, ActionType.PASS_TURN, "Passed turn", LocalDateTime.now());

        boolean allPlayersPassed = gm().passTurn();
        actionManager.recordAndSaveMove(move);

        if (allPlayersPassed) {
            PlayerType playerType = ApplicationConfiguration.getInstance().getPlayerType();
            if (playerType == PlayerType.HOST || playerType == PlayerType.LOCAL) {
                gameFlowManager.handleEndOfActionPhase();
            } else {
                log.info("CLIENT: All players passed, waiting for server to change phase.");
            }
        } else {
            controller.setViewedPlayer(gm().getCurrentPlayer());
            controller.updateAllUI();
        }
    }

    public void handlePlayCard(Card card) {
        Player currentPlayer = gm().getCurrentPlayer();

        if (!currentPlayer.canPlayCard(card)) {
            log.warn("Player {} cannot play card '{}'. Requirements not met or insufficient funds.",
                    currentPlayer.getName(), card.getName());
            return;
        }

        GameMove move = new GameMove(currentPlayer.getName(), ActionType.PLAY_CARD,
                card.getName(), LocalDateTime.now());

        if (card.getTileToPlace() != null) {
            if (isLocalPlayerMove(currentPlayer)) {
                controller.getPlacementManager().enterPlacementModeForCard(card, move);
            } else {
                currentPlayer.playCard(card, gm());
            }
        } else {
            currentPlayer.playCard(card, gm());
            actionManager.performAction();
            actionManager.recordAndSaveMove(move);
        }
    }

    public void handleClaimMilestone(Milestone milestone) {
        Player currentPlayer = gm().getCurrentPlayer();
        final int MILESTONE_COST = 8;

        if (board().claimMilestone(milestone, currentPlayer)) {
            currentPlayer.spendMC(MILESTONE_COST);
            actionManager.performAction();
            GameMove move = new GameMove(currentPlayer.getName(), ActionType.CLAIM_MILESTONE,
                    milestone.name(), LocalDateTime.now());
            actionManager.recordAndSaveMove(move);
        } else {
            log.warn("Failed attempt by {} to claim milestone '{}'.",
                    currentPlayer.getName(), milestone.getName());
        }
    }

    public void handleStandardProject(StandardProject project) {
        Player currentPlayer = gm().getCurrentPlayer();
        int finalCost = CostService.getFinalProjectCost(project, currentPlayer);

        if (currentPlayer.getMC() < finalCost) {
            log.warn("{} failed to use standard project '{}': insufficient MC (has {}, needs {}).",
                    currentPlayer.getName(), project.getName(), currentPlayer.getMC(), project.getCost());
            return;
        }

        GameMove move = new GameMove(gm().getCurrentPlayer().getName(),
                ActionType.USE_STANDARD_PROJECT, project.name(), LocalDateTime.now());

        if (project.requiresTilePlacement()) {
            if (isLocalPlayerMove(currentPlayer)) {
                controller.getPlacementManager().enterPlacementModeForProject(project, move);
            } else {
                currentPlayer.spendMC(finalCost);
            }
        } else {
            if (project == StandardProject.SELL_PATENTS) {
                if (currentPlayer.getHand().isEmpty()) {
                    log.warn("{} tried to sell patents but has no cards in hand.", currentPlayer.getName());
                    return;
                }
                if (isLocalPlayerMove(currentPlayer)) {
                    actionManager.openSellPatentsWindow();
                }
            } else {
                currentPlayer.spendMC(finalCost);
                project.execute(currentPlayer, board());
                actionManager.performAction();
                actionManager.recordAndSaveMove(move);
            }
        }
    }

    public void handleConvertHeat() {
        Player currentPlayer = gm().getCurrentPlayer();

        if (currentPlayer.resourceProperty(ResourceType.HEAT).get() < 8) {
            log.warn("{} failed to convert heat: insufficient resources.", currentPlayer.getName());
            return;
        }

        log.info("{} is converting 8 heat.", currentPlayer.getName());

        currentPlayer.resourceProperty(ResourceType.HEAT).set(
                currentPlayer.resourceProperty(ResourceType.HEAT).get() - 8
        );
        board().increaseTemperature();
        currentPlayer.increaseTR(1);
        actionManager.performAction();

        GameMove move = new GameMove(currentPlayer.getName(), ActionType.CONVERT_HEAT,
                "Raise temperature", LocalDateTime.now());
        actionManager.recordAndSaveMove(move);
    }

    public void handleConvertPlants() {
        Player currentPlayer = gm().getCurrentPlayer();
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
            controller.getPlacementManager().enterPlacementModeForPlant(convertPlantsMove);
        } else {
            currentPlayer.spendPlantsForGreenery();
        }
    }
}