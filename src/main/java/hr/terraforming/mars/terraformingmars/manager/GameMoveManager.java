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
public record GameMoveManager(ActionManager actionManager) {

    private GameManager gm() {
        return actionManager.getController().getGameManager();
    }

    private GameBoard board() {
        return actionManager.getController().getGameBoard();
    }

    private GameFlowManager gameFlow() {
        return actionManager.getGameFlowManager();
    }

    public void processMove(GameMove move) {

        if (move.actionType() != ActionType.OPEN_CHOOSE_CARDS_MODAL) {
            gm().setCurrentPlayerByName(move.playerName());
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

            case FINISH_FINAL_GREENERY -> handleFinishFinalGreenery(move);

            default -> log.warn("Unhandled action type: {}", move.actionType());
        }
    }

    private void handlePlaceTile(GameMove move) {
        if (ApplicationConfiguration.getInstance().getPlayerType() != PlayerType.HOST) {
            return;
        }

        Player player = gm().getPlayerByName(move.playerName());
        Tile tile = board().getTileAt(move.row(), move.col());

        if (tile != null && player != null) {
            switch (move.tileType()) {
                case OCEAN -> board().placeOcean(tile, player);
                case GREENERY -> board().placeGreenery(tile, player);
                case CITY -> board().placeCity(tile, player);
                default -> log.warn("Received PLACE_TILE with unhandled type: {}", move.tileType());
            }
            actionManager.performAction();
        }
    }

    private void handleSellPatents(GameMove move) {
        Player player = gm().getPlayerByName(move.playerName());
        if (player != null && !player.getHand().isEmpty()) {
            List<String> soldCardNames = Arrays.asList(move.details().split(","));

            player.getHand().removeIf(card -> soldCardNames.contains(card.getName()));
            player.spendMC(-soldCardNames.size());

            actionManager.performAction();
            log.info("{} sold: {}", player.getName(), move.details());
        }
    }

    private void handleFinishFinalGreenery(GameMove move) {
        log.info("Processing FINISH_FINAL_GREENERY from {}", move.playerName());

        FinalGreeneryPhaseManager finalGreeneryMgr = gameFlow().getFinalGreeneryManager();

        if (finalGreeneryMgr != null) {
            finalGreeneryMgr.finishForCurrentPlayer();
        } else {
            log.error("FinalGreeneryPhaseManager is null - cannot finish final greenery!");
        }
    }
}
