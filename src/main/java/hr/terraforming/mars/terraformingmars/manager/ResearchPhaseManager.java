package hr.terraforming.mars.terraformingmars.manager;

import hr.terraforming.mars.terraformingmars.controller.ChooseCardsController;
import hr.terraforming.mars.terraformingmars.controller.TerraformingMarsController;
import hr.terraforming.mars.terraformingmars.enums.ActionType;
import hr.terraforming.mars.terraformingmars.enums.PlayerType;
import hr.terraforming.mars.terraformingmars.model.*;
import hr.terraforming.mars.terraformingmars.util.ScreenLoader;
import javafx.application.Platform;
import javafx.stage.Window;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;

@Slf4j
public class ResearchPhaseManager {

    private final GameManager gameManager;
    private final Window ownerWindow;
    private final Runnable onResearchComplete;
    private int researchPlayerIndex = 0;
    private final TerraformingMarsController controller;

    public ResearchPhaseManager(GameManager gameManager, Window ownerWindow, TerraformingMarsController controller,Runnable onResearchComplete) {
        this.gameManager = gameManager;
        this.ownerWindow = ownerWindow;
        this.controller = controller;
        this.onResearchComplete = onResearchComplete;
    }

    public void start() {
        this.researchPlayerIndex = 0;
        log.info("🎬 ResearchPhaseManager starting from index 0");
        Platform.runLater(this::showScreenForNextPlayer);
    }

    private void showScreenForNextPlayer() {
        log.info("🎯 showScreenForNextPlayer called: researchPlayerIndex={}, totalPlayers={} | currentPhase={}",
                researchPlayerIndex, gameManager.getPlayers().size(), gameManager.getCurrentPhase());

        if (researchPlayerIndex >= gameManager.getPlayers().size()) {
            log.info("🏁 All players finished research! Calling onResearchComplete | currentPhase={}",
                    gameManager.getCurrentPhase());
            onResearchComplete.run();
            return;
        }

        Player currentPlayer = gameManager.getPlayers().get(researchPlayerIndex);
        log.info("👤 Current player for research: {}", currentPlayer.getName());

        String myPlayerName = ApplicationConfiguration.getInstance().getMyPlayerName();
        PlayerType playerType = ApplicationConfiguration.getInstance().getPlayerType();

        if (playerType != PlayerType.LOCAL && !currentPlayer.getName().equals(myPlayerName)) {
            log.info("Waiting for {} to complete research phase", currentPlayer.getName());
            return;
        }
        log.info("🎴 Opening modal for {}", currentPlayer.getName());

        List<Card> offer = gameManager.drawCards(4);

        if (offer.isEmpty()) {
            finishForCurrentPlayer(Collections.emptyList());
            return;
        }

        ScreenLoader.showAsModal(
                ownerWindow,
                "ChooseCards.fxml",
                "Research Phase - " + currentPlayer.getName(),
                0.7,
                0.8,
                (ChooseCardsController c) -> c.setup(currentPlayer, offer, this::finishForCurrentPlayer, gameManager, true)
        );
    }

    private void finishForCurrentPlayer(List<Card> boughtCards) {
        if (researchPlayerIndex >= gameManager.getPlayers().size()) {
            log.warn("⚠️ finishForCurrentPlayer called but research already complete, ignoring");
            return;
        }

        Player currentPlayer = gameManager.getPlayers().get(researchPlayerIndex);
        log.info("✅ {} finished research, bought {} cards | currentPhase={}",
                currentPlayer.getName(), boughtCards.size(), gameManager.getCurrentPhase());

        if (!boughtCards.isEmpty()) {
            String details = boughtCards.stream().map(Card::getName).reduce((a,b) -> a + "," + b).orElse("");
            GameMove modalMove = new GameMove(
                    currentPlayer.getName(),
                    ActionType.OPEN_CHOOSE_CARDS_MODAL,
                    details,
                    java.time.LocalDateTime.now()
            );
            controller.getActionManager().recordAndSaveMove(modalMove);
        }

        int cost = boughtCards.size() * 3;
        if (currentPlayer.spendMC(cost)) {
            currentPlayer.getHand().addAll(boughtCards);
        }

        String myPlayerName = ApplicationConfiguration.getInstance().getMyPlayerName();
        if (currentPlayer.getName().equals(myPlayerName)) {
            log.info("➡️ HOST calling advanceDraftPlayer() for local player {} | currentPhase={}",
                    currentPlayer.getName(), gameManager.getCurrentPhase());
            gameManager.advanceDraftPlayer();
        }

        researchPlayerIndex++;
        log.info("➡️ Moving to next player (researchPlayerIndex now: {}) | currentPhase={}",
                researchPlayerIndex, gameManager.getCurrentPhase());

        Platform.runLater(this::showScreenForNextPlayer);
    }
}