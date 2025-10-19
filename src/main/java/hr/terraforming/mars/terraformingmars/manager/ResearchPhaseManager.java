package hr.terraforming.mars.terraformingmars.manager;

import hr.terraforming.mars.terraformingmars.controller.ChooseCardsController;
import hr.terraforming.mars.terraformingmars.controller.TerraformingMarsController;
import hr.terraforming.mars.terraformingmars.enums.ActionType;
import hr.terraforming.mars.terraformingmars.model.Card;
import hr.terraforming.mars.terraformingmars.model.GameManager;
import hr.terraforming.mars.terraformingmars.model.GameMove;
import hr.terraforming.mars.terraformingmars.model.Player;
import hr.terraforming.mars.terraformingmars.util.ScreenLoader;
import javafx.application.Platform;
import javafx.stage.Window;
import java.util.Collections;
import java.util.List;

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
                (ChooseCardsController c) -> c.setup(currentPlayer, offer, this::finishForCurrentPlayer)
        );
    }

    private void finishForCurrentPlayer(List<Card> boughtCards) {
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
        researchPlayerIndex++;
        Platform.runLater(this::showScreenForNextPlayer);
    }
}