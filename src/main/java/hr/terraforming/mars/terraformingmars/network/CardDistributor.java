package hr.terraforming.mars.terraformingmars.network;

import hr.terraforming.mars.terraformingmars.controller.game.ChooseCardsController;
import hr.terraforming.mars.terraformingmars.manager.ActionManager;
import hr.terraforming.mars.terraformingmars.model.*;
import hr.terraforming.mars.terraformingmars.network.message.CorporationOfferMessage;
import hr.terraforming.mars.terraformingmars.network.message.CardOfferMessage;
import hr.terraforming.mars.terraformingmars.util.ScreenUtils;
import hr.terraforming.mars.terraformingmars.view.ScreenNavigator;
import javafx.application.Platform;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public record CardDistributor(GameManager gameManager, GameServerThread serverThread, ActionManager actionManager) {

    public void distributeInitialCorporations() {
        log.debug("Host distributing corporations to all players...");
        gameManager.shuffleCorporations();

        for (Player player : gameManager.getPlayers()) {
            List<Corporation> offer = gameManager.drawCorporations(2);
            if (isLocalHost(player)) {
                Platform.runLater(() -> ScreenNavigator.showChooseCorporationScreen(player, offer, gameManager));
            } else {
                List<String> names = offer.stream().map(Corporation::name).toList();
                serverThread.sendToPlayer(player.getName(), new CorporationOfferMessage(player.getName(), names));
            }
        }
    }

    public void distributeInitialCards() {
        log.debug("Host distributing initial project cards...");
        gameManager.shuffleCards();

        for (Player player : gameManager.getPlayers()) {
            List<Card> offer = gameManager.drawCards(6);
            if (isLocalHost(player)) {
                Platform.runLater(() -> ScreenNavigator.showInitialCardDraftScreen(player, offer, gameManager));
            } else {
                List<String> names = offer.stream().map(Card::getName).toList();
                serverThread.sendToPlayer(player.getName(), new CardOfferMessage(player.getName(), names));
            }
        }
    }

    public void distributeResearchCards() {
        log.debug("Host distributing Research Phase cards...");
        for (Player player : gameManager.getPlayers()) {
            List<Card> offer = gameManager.drawCards(4);

            if (offer.isEmpty()) {
                if (actionManager != null && actionManager.getGameFlowManager() != null) {
                    Platform.runLater(() -> actionManager.getGameFlowManager().onResearchComplete());
                }
                continue;
            }

            if (isLocalHost(player)) {
                Platform.runLater(() -> ScreenUtils.showAsModal(
                        ScreenNavigator.getMainStage(), "ChooseCards.fxml", "Research Phase", 0.7, 0.8,
                        (ChooseCardsController c) -> c.setup(player, offer, cards -> handleHostConfirmation(player, cards), gameManager, true)
                ));
            } else {
                List<String> names = offer.stream().map(Card::getName).toList();
                serverThread.sendToPlayer(player.getName(), new CardOfferMessage(player.getName(), names));
            }
        }
    }

    private void handleHostConfirmation(Player player, List<Card> boughtCards) {
        log.debug("Host confirming research cards. Count: {}", boughtCards.size());
        int cost = boughtCards.size() * 3;
        if (player.getMC() >= cost) {
            player.spendMC(cost);
            player.getHand().addAll(boughtCards);
        }

        synchronized (gameManager) {
            boolean morePlayers = gameManager.advanceDraftPlayer();
            if (!morePlayers) {
                log.info("Host finished last. Triggering next phase (ACTIONS).");
                if (actionManager != null && actionManager.getGameFlowManager() != null) {
                    actionManager.getGameFlowManager().onResearchComplete();
                }
            }
        }
    }

    private boolean isLocalHost(Player player) {
        return player.getName().equals(ApplicationConfiguration.getInstance().getMyPlayerName());
    }
}
