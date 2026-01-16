package hr.terraforming.mars.terraformingmars.network;

import hr.terraforming.mars.terraformingmars.enums.ActionType;
import hr.terraforming.mars.terraformingmars.exception.GameStateException;
import hr.terraforming.mars.terraformingmars.factory.CardFactory;
import hr.terraforming.mars.terraformingmars.factory.CorporationFactory;
import hr.terraforming.mars.terraformingmars.manager.ActionManager;
import hr.terraforming.mars.terraformingmars.model.*;
import hr.terraforming.mars.terraformingmars.network.message.CardChoiceMessage;
import hr.terraforming.mars.terraformingmars.network.message.CorporationChoiceMessage;
import hr.terraforming.mars.terraformingmars.network.message.PlayerNameMessage;
import hr.terraforming.mars.terraformingmars.util.GameMoveUtils;
import javafx.application.Platform;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Objects;

@Slf4j
public record ServerMessageHandler(GameManager gameManager, ActionManager actionManager, Runnable broadcaster) {

    public String handlePlayerName(PlayerNameMessage msg) {
        String playerName = msg.playerName();

        for (Player player : gameManager.getPlayers()) {
            if (player.getName().startsWith("Player ")) {
                player.setName(playerName);
                broadcaster.run();
                break;
            }
        }
        return playerName;
    }

    public void handleCorporationChoice(String playerName, CorporationChoiceMessage msg) {
        Corporation corporation = CorporationFactory.getCorporationByName(msg.corporationName());

        if (corporation == null) {
            throw new GameStateException("Unknown corporation choice: " + msg.corporationName() + "' does not exist");
        }

        Player playerByName = gameManager.getPlayerByName(playerName);

        if (playerByName != null) {
            playerByName.setCorporation(corporation);
        }

        broadcaster.run();

        boolean allPlayersChoseCorporations = gameManager.getPlayers().stream()
                .allMatch(player -> player.getCorporation() != null);

        if (allPlayersChoseCorporations) {
            log.debug("All of the players have chosen corporations. Distributing initial cards...");
        }
    }

    public void handleCardChoice(String playerName, CardChoiceMessage msg) {
        Player player = gameManager.getPlayerByName(playerName);

        if (player == null) {
            throw new GameStateException("Unknown player '" + playerName + "' sent card choice");
        }

        List<Card> boughtCards = msg.cardNames().stream()
                .map(CardFactory::getCardByName)
                .filter(Objects::nonNull)
                .toList();

        int cost = boughtCards.size() * 3;

        if (player.getMC() < cost) {
            throw new GameStateException("Player '" + playerName + "' tried to buy cards without enough MC");
        }

        player.canSpendMC(cost);
        player.getHand().addAll(boughtCards);

        synchronized (gameManager) {
            boolean morePlayersToChoose = gameManager.hasMoreDraftPlayers();

            if (!morePlayersToChoose) {
                if (gameManager.getGeneration() == 0) {
                    GameMoveUtils.saveInitialSetupMove(gameManager);
                    gameManager.startGame();
                } else {
                    Platform.runLater(() -> {
                        var flowManager = actionManager.getGameFlowManager();
                        if (flowManager != null) {
                            flowManager.finishResearchPhase();
                        }
                    });
                }
            }
        }

        broadcaster.run();
    }

    public void handleGameMove(GameMove move) {
        if (actionManager == null) {
            log.debug("ActionManager is null, cannot process move");
            return;
        }

        Platform.runLater(() -> {
            try {
                actionManager.processMove(move);
                if (shouldBroadcastAfterMove(move.actionType())) {
                    broadcaster.run();
                }
            } catch (Exception e) {
                log.error("Error processing move on FX thread", e);
            }
        });
    }

    private boolean shouldBroadcastAfterMove(ActionType type) {
        return type == ActionType.PLACE_TILE || type == ActionType.SELL_PATENTS;
    }
}
