package hr.terraforming.mars.terraformingmars.manager;

import hr.terraforming.mars.terraformingmars.controller.ChooseCardsController;
import hr.terraforming.mars.terraformingmars.enums.CardSelectionContext;
import hr.terraforming.mars.terraformingmars.model.Card;
import hr.terraforming.mars.terraformingmars.model.GameManager;
import hr.terraforming.mars.terraformingmars.model.Player;
import hr.terraforming.mars.terraformingmars.util.ScreenLoader;
import javafx.application.Platform;
import javafx.stage.Window;
import java.util.Collections;
import java.util.List;

public class ResearchPhaseManager {

    private final GameManager gameManager;
    private final Runnable onResearchComplete;
    private final Window ownerWindow;
    private int researchPlayerIndex = 0;

    public ResearchPhaseManager(GameManager gameManager, Window ownerWindow, Runnable onResearchComplete) {
        this.gameManager = gameManager;
        this.ownerWindow = ownerWindow;
        this.onResearchComplete = onResearchComplete;
    }

    public void start() {
        this.researchPlayerIndex = 0;
        Platform.runLater(this::showScreenForNextPlayer);
    }

    private void showScreenForNextPlayer() {
        if (researchPlayerIndex >= gameManager.getPlayers().size()) {
            onResearchComplete.run();
            return;
        }

        Player currentPlayer = gameManager.getPlayers().get(researchPlayerIndex);
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
                (ChooseCardsController c) -> c.setup(currentPlayer, offer, CardSelectionContext.RESEARCH, this::finishForCurrentPlayer)
        );

    }

    private void finishForCurrentPlayer(List<Card> boughtCards) {
        Player currentPlayer = gameManager.getPlayers().get(researchPlayerIndex);
        int cost = boughtCards.size() * 3;
        if (currentPlayer.spendMC(cost)) {
            currentPlayer.getHand().addAll(boughtCards);
        }
        researchPlayerIndex++;
        Platform.runLater(this::showScreenForNextPlayer);
    }
}