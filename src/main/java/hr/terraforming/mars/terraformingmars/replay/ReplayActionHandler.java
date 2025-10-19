package hr.terraforming.mars.terraformingmars.replay;

import hr.terraforming.mars.terraformingmars.controller.*;
import hr.terraforming.mars.terraformingmars.enums.*;
import hr.terraforming.mars.terraformingmars.factory.CardFactory;
import hr.terraforming.mars.terraformingmars.model.GameManager;
import hr.terraforming.mars.terraformingmars.model.*;
import hr.terraforming.mars.terraformingmars.service.CostService;
import hr.terraforming.mars.terraformingmars.util.ScreenLoader;
import hr.terraforming.mars.terraformingmars.view.GameScreens;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public record ReplayActionHandler(TerraformingMarsController controller, ReplayLoader loader) {

    private static final Logger logger = LoggerFactory.getLogger(ReplayActionHandler.class);

    void executeReplayMove(GameMove move) {
        GameManager gameManager = controller.getGameManager();

        switch (move.getActionType()) {
            case OPEN_CHOOSE_CARDS_MODAL -> {
                Platform.runLater(() -> {
                    List<String> names = Arrays.asList(move.getDetails().split(","));
                    ScreenLoader.showAsModal(
                            controller.getSceneWindow(),
                            "ChooseCards.fxml",
                            "Research (Replay)",
                            0.7, 0.8,
                            (ChooseCardsController c) -> c.replayShowChosenCards(names)
                    );
                });
                return;
            }
            case OPEN_SELL_PATENTS_MODAL -> {
                Platform.runLater(() -> {
                    List<String> soldCardNames = Arrays.asList(move.getDetails().split(","));

                    Player playerForReplay = gameManager.getPlayers().stream()
                            .filter(p -> p.getName().equals(move.getPlayerName()))
                            .findFirst().orElse(null);

                    if (playerForReplay != null) {
                        List<Card> handBeforeSale = new ArrayList<>(playerForReplay.getHand());

                        ScreenLoader.showAsModal(
                                controller.getSceneWindow(),
                                "SellPatents.fxml",
                                "Sell Patents (Replay)",
                                0.5, 0.7,
                                (SellPatentsController c) -> c.replayShowSoldPatents(soldCardNames, handBeforeSale)
                        );
                    }
                });
                return;
            }
            case OPEN_FINAL_GREENERY_MODAL -> {
                Platform.runLater(() -> {
                    String[] parts = move.getDetails().split(",");
                    String playerName = parts[0];
                    int plants = Integer.parseInt(parts[1]);
                    int cost = Integer.parseInt(parts[2]);
                    ScreenLoader.showAsModal(
                            controller.getSceneWindow(),
                            "FinalGreeneryScreen.fxml",
                            "Final Greenery (Replay)",
                            0.4, 0.5,
                            (FinalGreeneryController c) -> c.replayShowFinalGreenery(playerName, plants, cost)
                    );
                });
                return;
            }
            default -> { /*Nothing is done*/ }
        }

        if ("System".equals(move.getPlayerName())) {
            handleSystemMove(move, gameManager);
        } else {
            handlePlayerMove(move, gameManager);
        }

        controller.updateAllUI();
    }

    private void handleSystemMove(GameMove move, GameManager gameManager) {
        if (move.getActionType() == ActionType.RESEARCH_COMPLETE) {
            gameManager.doProduction();
            gameManager.startNewGeneration();
            loader.updatePlayerHandsFromDetails(gameManager, move.getDetails());
            controller.setViewedPlayer(gameManager.getCurrentPlayer());
        }
    }

    private void handlePlayerMove(GameMove move, GameManager gameManager) {
        Player player = gameManager.getPlayers().stream()
                .filter(p -> p.getName().equals(move.getPlayerName()))
                .findFirst().orElse(null);

        if (player == null) {
            logger.error("Replay Error: Player {} not found.", move.getPlayerName());
            return;
        }

        gameManager.setCurrentPlayerByName(player.getName());
        controller.setViewedPlayer(player);

        controller.updateLastMoveLabel(move);

        switch (move.getActionType()) {
            case PLACE_TILE -> processPlaceTile(move, player);
            case PLAY_CARD -> processPlayCard(move, player, gameManager);
            case CLAIM_MILESTONE -> processClaimMilestone(move, player);
            case USE_STANDARD_PROJECT -> processUseStandardProject(move, player);
            case CONVERT_HEAT -> processConvertHeat(player);
            case CONVERT_PLANTS -> player.spendPlantsForGreenery(); // Simple enough to remain inline
            case SELL_PATENTS -> processSellPatents(move, player);
            case PASS_TURN -> {
                boolean allPlayersPassed = gameManager.passTurn();
                if (!allPlayersPassed) {
                    controller.setViewedPlayer(gameManager.getCurrentPlayer());
                }
            }
            default -> logger.error("Unknown ActionType in replay: {}.", move.getActionType());
        }
    }

    private void processPlaceTile(GameMove move, Player player) {
        GameBoard gameBoard = controller.getGameBoard();
        Tile tileToPlaceOn = gameBoard.getTileAt(move.getRow(), move.getCol());
        if (tileToPlaceOn != null) {
            switch (move.getTileType()) {
                case OCEAN -> gameBoard.placeOcean(tileToPlaceOn, player);
                case GREENERY -> gameBoard.placeGreenery(tileToPlaceOn, player);
                case CITY -> gameBoard.placeCity(tileToPlaceOn, player);
                default -> logger.warn("Replay: Invalid tile type {} for placement.", move.getTileType());
            }
        }
    }

    private void processPlayCard(GameMove move, Player player, GameManager gameManager) {
        Card card = CardFactory.getCardByName(move.getDetails());
        if (card != null) {
            player.playCard(card, gameManager);
        }
    }

    private void processClaimMilestone(GameMove move, Player player) {
        try {
            Milestone milestone = Milestone.valueOf(move.getDetails().toUpperCase());
            player.spendMC(8);
            controller.getGameBoard().claimMilestone(milestone, player);
        } catch (IllegalArgumentException e) {
            logger.error("Replay Error: Invalid Milestone name '{}' in game move.", move.getDetails(), e);
        }
    }

    private void processUseStandardProject(GameMove move, Player player) {
        StandardProject project = StandardProject.valueOf(move.getDetails());
        int finalCost = CostService.getFinalProjectCost(project, player);
        player.spendMC(finalCost);
        project.execute(player, controller.getGameBoard());
    }

    private void processConvertHeat(Player player) {
        player.addResource(ResourceType.HEAT, -8);
        if (controller.getGameBoard().increaseTemperature()) {
            player.increaseTR(1);
        }
    }

    private void processSellPatents(GameMove move, Player player) {
        try {
            String numberStr = move.getDetails().replaceAll("\\D", "");
            player.addMC(Integer.parseInt(numberStr));
        } catch (NumberFormatException _) {
            logger.warn("Could not parse number of sold patents from details: {}", move.getDetails());
        }
    }

    public void showNoMovesToReplayAlert() {
        new Alert(Alert.AlertType.INFORMATION, "No game moves found to replay.").show();
    }

    public void showGameOverScreen(List<Player> rankedPlayers) {
        Platform.runLater(() -> GameScreens.showGameOverScreen(rankedPlayers));
    }

    public void clearLastMoveLabel() {
        controller.updateLastMoveLabel(null);
    }
}