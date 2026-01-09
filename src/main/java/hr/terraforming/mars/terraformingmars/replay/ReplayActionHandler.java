package hr.terraforming.mars.terraformingmars.replay;

import hr.terraforming.mars.terraformingmars.controller.game.*;
import hr.terraforming.mars.terraformingmars.enums.*;
import hr.terraforming.mars.terraformingmars.factory.CardFactory;
import hr.terraforming.mars.terraformingmars.model.GameManager;
import hr.terraforming.mars.terraformingmars.model.*;
import hr.terraforming.mars.terraformingmars.service.CostService;
import hr.terraforming.mars.terraformingmars.model.ProductionReport;
import hr.terraforming.mars.terraformingmars.service.ProductionReportService;
import hr.terraforming.mars.terraformingmars.util.ScreenUtils;
import javafx.application.Platform;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
public record ReplayActionHandler(GameScreenController controller, ReplayLoader loader) {

    void executeReplayMove(GameMove move) {
        GameManager gameManager = controller.getGameManager();

        switch (move.actionType()) {
            case OPEN_CHOOSE_CARDS_MODAL -> {
                Platform.runLater(() -> {
                    List<String> cardNames = Arrays.asList(move.details().split(", "));
                    String playerName = move.playerName();

                    ScreenUtils.showAsModal(
                            controller.getSceneWindow(),
                            "ChooseCards.fxml",
                            "Research (Replay)",
                            0.7, 0.8,
                            (ChooseCardsController c) -> c.replayShowChosenCards(cardNames, playerName)
                    );
                });
                return;
            }
            case OPEN_SELL_PATENTS_MODAL -> {
                Platform.runLater(() -> {
                    List<String> soldCardNames = Arrays.asList(move.details().split(", "));
                    String playerName = move.playerName();
                    Player playerForReplay = gameManager.getPlayerByName(playerName);

                    if (playerForReplay != null) {
                        List<Card> handBeforeSale = new ArrayList<>(playerForReplay.getHand());

                        ScreenUtils.showAsModal(
                                controller.getSceneWindow(),
                                "SellPatents.fxml",
                                "Sell Patents (Replay)",
                                0.5, 0.7,
                                (SellPatentsController c) -> c.replayShowSoldPatents(soldCardNames, handBeforeSale, playerName)
                        );
                    }
                });
                return;
            }
            case OPEN_FINAL_GREENERY_MODAL -> {
                Platform.runLater(() -> {
                    String[] parts = move.details().split(",");
                    String playerName = parts[0];
                    int plants = Integer.parseInt(parts[1]);
                    int cost = Integer.parseInt(parts[2]);
                    ScreenUtils.showAsModal(
                            controller.getSceneWindow(),
                            "FinalGreenery.fxml",
                            "Final Greenery (Replay)",
                            0.4, 0.5,
                            (FinalGreeneryController c) -> c.replayShowFinalGreenery(playerName, plants, cost)
                    );
                });
                return;
            }
            case OPEN_PRODUCTION_PHASE_MODAL -> {
                Platform.runLater(() -> showProductionPhaseInReplay(move, gameManager));
                return;
            }
            default -> { /*Nothing happens*/ }
        }

        if ("System".equals(move.playerName())) {
            handleSystemMove(move, gameManager);
        } else {
            handlePlayerMove(move, gameManager);
        }

        controller.updateAllUI();
    }

    private void handleSystemMove(GameMove move, GameManager gameManager) {
        if (move.actionType() == ActionType.RESEARCH_COMPLETE) {
            gameManager.doProduction();
            gameManager.startNewGeneration();
            loader.updatePlayerHandsFromDetails(gameManager, move.details());
            controller.setViewedPlayer(gameManager.getCurrentPlayer());
        }
    }

    private void handlePlayerMove(GameMove move, GameManager gameManager) {
        Player player = gameManager.getPlayerByName(move.playerName());

        if (player == null) {
            log.error("Replay Error: Player {} not found.", move.playerName());
            return;
        }

        gameManager.setCurrentPlayerByName(player.getName());
        controller.setViewedPlayer(player);

        controller.updateLastMoveLabel(move);

        switch (move.actionType()) {
            case PLACE_TILE -> processPlaceTile(move, player);
            case PLAY_CARD -> processPlayCard(move, player, gameManager);
            case CLAIM_MILESTONE -> processClaimMilestone(move, player);
            case USE_STANDARD_PROJECT -> processUseStandardProject(move, player);
            case CONVERT_HEAT -> processConvertHeat(player);
            case CONVERT_PLANTS -> player.spendPlantsForGreenery();
            case SELL_PATENTS -> processSellPatents(move, player);
            case PASS_TURN -> {
                boolean allPlayersPassed = gameManager.passTurn();
                if (!allPlayersPassed) {
                    controller.setViewedPlayer(gameManager.getCurrentPlayer());
                }
            }
            default -> log.error("Unknown ActionType in replay: {}.", move.actionType());
        }
    }

    private void processPlaceTile(GameMove move, Player player) {
        GameBoard gameBoard = controller.getGameBoard();
        Tile tileToPlaceOn = gameBoard.getTileAt(move.row(), move.col());
        if (tileToPlaceOn != null) {
            switch (move.tileType()) {
                case OCEAN -> gameBoard.placeOcean(tileToPlaceOn, player);
                case GREENERY -> gameBoard.placeGreenery(tileToPlaceOn, player);
                case CITY -> gameBoard.placeCity(tileToPlaceOn, player);
                default -> log.warn("Replay: Invalid tile type {} for placement.", move.tileType());
            }
        }
    }

    private void processPlayCard(GameMove move, Player player, GameManager gameManager) {
        Card card = CardFactory.getCardByName(move.details());
        if (card != null) {
            player.playCard(card, gameManager);
        }
    }

    private void processClaimMilestone(GameMove move, Player player) {
        try {
            Milestone milestone = Milestone.valueOf(move.details());
            player.canSpendMC(8);
            controller.getGameBoard().canClaimMilestone(milestone, player);
        } catch (IllegalArgumentException e) {
            log.error("Replay Error: Invalid Milestone name '{}' in game move.", move.details(), e);
        }
    }

    private void processUseStandardProject(GameMove move, Player player) {
        StandardProject project = StandardProject.valueOf(move.details());
        int finalCost = CostService.getFinalProjectCost(project, player);
        player.canSpendMC(finalCost);
        project.execute(player, controller.getGameBoard());
    }

    private void processConvertHeat(Player player) {
        player.addResource(ResourceType.HEAT, -8);
        if (controller.getGameBoard().canIncreaseTemperature()) {
            player.increaseTR(1);
        }
    }

    private void processSellPatents(GameMove move, Player player) {
        try {
            String[] soldCardNames = move.details().split(", ");
            player.addMC(soldCardNames.length);
        } catch (NumberFormatException _) {
            log.warn("Could not parse number of sold patents from details: {}", move.details());
        }
    }

    public void clearLastMoveLabel() {
        controller.updateLastMoveLabel(null);
    }

    private void showProductionPhaseInReplay(GameMove move, GameManager gameManager) {
        try {
            int generation = Integer.parseInt(move.details());
            List<ProductionReport> summaries = ProductionReportService.generateSummaries(gameManager);

            ScreenUtils.showAsModal(
                    controller.getSceneWindow(),
                    "ProductionPhase.fxml",
                    "Production Phase - Generation " + generation + " (Replay)",
                    0.5, 0.6,
                    (ProductionPhaseController c) -> c.replayShowProductionSummary(summaries, generation)
            );

            log.info("Production Phase modal shown in replay for Generation {}", generation);
        } catch (Exception e) {
            log.error("Failed to show Production Phase modal in replay", e);
        }
    }
}