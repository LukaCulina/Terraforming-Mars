package hr.terraforming.mars.terraformingmars.network;

import hr.terraforming.mars.terraformingmars.enums.ActionType;
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
public record ClientGameMessageHandler(GameManager gameManager, ActionManager actionManager, Runnable broadcaster) {

    public String handlePlayerName(PlayerNameMessage msg) {
        String playerName = msg.playerName();
        log.info("Associating client handler with player: {}", playerName);

        for (Player p : gameManager.getPlayers()) {
            if (p.getName().startsWith("Player ")) {
                p.setName(playerName);
                broadcaster.run();
                break;
            }
        }
        return playerName;
    }

    public void handleCorporationChoice(String playerName, CorporationChoiceMessage msg) {
        Corporation corp = CorporationFactory.getCorporationByName(msg.corporationName());

        if (corp == null) {
            log.warn("Unknown corporation choice: {}", msg.corporationName());
            return;
        }

        Player p = gameManager.getPlayerByName(playerName);
        if (p != null) {
            p.setCorporation(corp);
        }
        broadcaster.run();

        boolean allChosen = gameManager.getPlayers().stream()
                .allMatch(player -> player.getCorporation() != null);

        if (allChosen) {
            log.info("All of the players have chosen corporations. Distributing initial cards...");
        }
    }

    public void handleCardChoice(String playerName, CardChoiceMessage msg) {
        Player player = gameManager.getPlayerByName(playerName);

        if (player == null) {
            log.error("Unknown player sent card choice: {}", playerName);
            return;
        }

        List<Card> boughtCards = msg.cardNames().stream()
                .map(CardFactory::getCardByName)
                .filter(Objects::nonNull)
                .toList();

        int cost = boughtCards.size() * 3;

        if (player.getMC() < cost) {
            log.warn("Player {} tried to buy cards without enough MC!", playerName);
            return;
        }

        player.spendMC(cost);
        player.getHand().addAll(boughtCards);

        synchronized (gameManager) {
            boolean morePlayers = gameManager.advanceDraftPlayer();

            if (!morePlayers) {
                if (gameManager.getGeneration() == 0) {
                    GameMoveUtils.saveInitialSetupMove(gameManager);
                    gameManager.startGame();
                } else {
                    Platform.runLater(() -> {
                        var flowManager = actionManager.getGameFlowManager();
                        if (flowManager != null) {
                            flowManager.onResearchComplete();
                        }
                    });
                }
            }
        }

        broadcaster.run();
    }

    public void handleGameMove(GameMove move) {
        if (actionManager == null) {
            log.warn("ActionManager is null, cannot process move");
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
