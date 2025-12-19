package hr.terraforming.mars.terraformingmars.manager;

import hr.terraforming.mars.terraformingmars.controller.game.ChooseCardsController;
import hr.terraforming.mars.terraformingmars.controller.game.GameScreenController;
import hr.terraforming.mars.terraformingmars.enums.ActionType;
import hr.terraforming.mars.terraformingmars.enums.PlayerType;
import hr.terraforming.mars.terraformingmars.model.*;
import hr.terraforming.mars.terraformingmars.util.ScreenUtils;
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
    private final GameScreenController controller;

    public ResearchPhaseManager(GameManager gameManager, Window ownerWindow, GameScreenController controller, Runnable onResearchComplete) {
        this.gameManager = gameManager;
        this.ownerWindow = ownerWindow;
        this.controller = controller;
        this.onResearchComplete = onResearchComplete;
    }

    public void start() {
        this.researchPlayerIndex = 0;
        Platform.runLater(this::showScreenForNextPlayer);
    }

    private void showScreenForNextPlayer() {
        if (researchPlayerIndex >= gameManager.getPlayers().size()) {
            log.info("All players finished research! Calling onResearchComplete | currentPhase={}",
                    gameManager.getCurrentPhase());
            onResearchComplete.run();
            return;
        }

        Player currentPlayer = gameManager.getPlayers().get(researchPlayerIndex);
        String myPlayerName = ApplicationConfiguration.getInstance().getMyPlayerName();
        PlayerType playerType = ApplicationConfiguration.getInstance().getPlayerType();

        if (playerType != PlayerType.LOCAL && !currentPlayer.getName().equals(myPlayerName)) {
            return;
        }

        List<Card> offer = gameManager.drawCards(4);

        if (offer.isEmpty()) {
            finishForCurrentPlayer(Collections.emptyList());
            return;
        }

        ScreenUtils.showAsModal(
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
            log.warn("finishForCurrentPlayer called but research has already been complete!");
            return;
        }

        Player currentPlayer = gameManager.getPlayers().get(researchPlayerIndex);

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
            gameManager.advanceDraftPlayer();
        }

        researchPlayerIndex++;

        Platform.runLater(this::showScreenForNextPlayer);
    }
}