package hr.terraforming.mars.terraformingmars.manager;

import hr.terraforming.mars.terraformingmars.enums.ActionType;
import hr.terraforming.mars.terraformingmars.enums.Milestone;
import hr.terraforming.mars.terraformingmars.enums.PlayerType;
import hr.terraforming.mars.terraformingmars.enums.StandardProject;
import hr.terraforming.mars.terraformingmars.factory.CardFactory;
import hr.terraforming.mars.terraformingmars.model.*;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;

@Slf4j
public record MoveManager(GameManager gameManager, GameBoard gameBoard, ActionManager actionManager) {

    public void processMove(GameMove move) {
        String currentPlayerName = gameManager.getCurrentPlayer().getName(); // ✅
        String movePlayerName = move.playerName();

        log.info("🔄 processMove() called: Player='{}', Action='{}', CurrentPlayer='{}'",
                movePlayerName, move.actionType(), currentPlayerName);

        if (move.actionType() != ActionType.OPEN_CHOOSE_CARDS_MODAL) {
            gameManager.setCurrentPlayerByName(move.playerName()); // ✅
        } else {
            log.info("Ignoring setCurrentPlayerByName for OPEN_CHOOSE_CARDS_MODAL");
        }

        switch (move.actionType()) {
            case PLAY_CARD -> {
                Card card = CardFactory.getCardByName(move.details());
                if (card != null) {
                    actionManager.handlePlayCard(card);
                } else {
                    log.warn("Card not found: {}", move.details());
                }
            }

            case PLACE_TILE -> handlePlaceTile(move);

            case SELL_PATENTS -> handleSellPatents(move);

            case CLAIM_MILESTONE -> {
                try {
                    Milestone milestone = Milestone.valueOf(move.details());
                    actionManager.handleClaimMilestone(milestone);
                } catch (IllegalArgumentException _) {
                    log.warn("Invalid milestone: {}", move.details());
                }
            }

            case USE_STANDARD_PROJECT -> {
                try {
                    StandardProject project = StandardProject.valueOf(move.details());
                    actionManager.handleStandardProject(project);
                } catch (IllegalArgumentException _) {
                    log.warn("Invalid standard project: {}", move.details());
                }
            }

            case CONVERT_HEAT -> actionManager.handleConvertHeat();

            case CONVERT_PLANTS -> actionManager.handleConvertPlants();

            case PASS_TURN -> actionManager.handlePassTurn();

            case RESEARCH_COMPLETE -> log.info("🔬 Processing RESEARCH_COMPLETE move");

            case OPEN_CHOOSE_CARDS_MODAL -> log.info("Network {} opened choose cards modal", move.playerName());

            case OPEN_SELL_PATENTS_MODAL -> log.info("Network: {} opened sell patents modal", move.playerName());

            default -> log.warn("Unhandled action type: {}", move.actionType());
        }
    }

    private void handlePlaceTile(GameMove move) {
        if (ApplicationConfiguration.getInstance().getPlayerType() != PlayerType.HOST) {
            return;
        }

        Player player = gameManager.getPlayerByName(move.playerName()); // ✅
        Tile tile = gameBoard.getTileAt(move.row(), move.col()); // ✅

        if (tile != null && player != null) {
            switch (move.tileType()) {
                case OCEAN -> gameBoard.placeOcean(tile, player); // ✅
                case GREENERY -> gameBoard.placeGreenery(tile, player); // ✅
                case CITY -> gameBoard.placeCity(tile, player); // ✅
                default -> log.warn("⚠️ Received PLACE_TILE with unhandled type: {}", move.tileType());
            }
            log.info("✅ Server calling performAction() after PLACE_TILE for {}", player.getName());
            actionManager.performAction();
        }
    }

    private void handleSellPatents(GameMove move) {
        Player player = gameManager.getPlayerByName(move.playerName()); // ✅
        if (player != null && !player.getHand().isEmpty()) {
            List<String> soldCardNames = Arrays.asList(move.details().split(","));

            player.getHand().removeIf(card -> soldCardNames.contains(card.getName()));
            player.spendMC(-soldCardNames.size());

            actionManager.performAction();
            log.info("Network: {} sold: {}", player.getName(), move.details());
        }
    }
}
