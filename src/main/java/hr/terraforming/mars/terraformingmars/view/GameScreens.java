package hr.terraforming.mars.terraformingmars.view;

import hr.terraforming.mars.terraformingmars.controller.*;
import hr.terraforming.mars.terraformingmars.enums.CardSelectionContext;
import hr.terraforming.mars.terraformingmars.model.*;
import hr.terraforming.mars.terraformingmars.util.ScreenLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.*;
import java.util.function.Consumer;

public class GameScreens {

    private GameScreens() { throw new IllegalStateException("Utility class"); }

    private static Stage mainStage;

    public static void setMainStage(Stage stage) {
        mainStage = stage;
    }

    public static void showChoosePlayersScreen() {
        ScreenLoader.showAsMainScreen(mainStage, "ChoosePlayers.fxml", "Choose the number of players", (ChoosePlayersController _) -> {});
    }

    public static void showChooseCorporationScreen(GameManager gameManager) {
        Player currentPlayer = gameManager.getCurrentPlayer();

        List<Corporation> offer = gameManager.getCorporationOffer();

        ScreenLoader.showAsMainScreen(mainStage, "ChooseCorporation.fxml", "Choose for " + currentPlayer.getName(),
                (ChooseCorporationController c) -> c.setCorporationOptions(currentPlayer, offer, gameManager));
    }

    public static void showInitialCardDraftScreen(GameManager gameManager) {
        Player currentPlayer = gameManager.getCurrentPlayerForDraft();
        List<Card> offer = gameManager.drawCards(6);

        Consumer<List<Card>> onConfirmAction = chosenCards -> {
            currentPlayer.getHand().addAll(chosenCards);

            if (gameManager.advanceDraftPlayer()) {
                showInitialCardDraftScreen(gameManager);
            } else {
                gameManager.startGame();
                GameState gameState = new GameState(gameManager, gameManager.getGameBoard());
                startGameWithChosenCards(gameState);
            }
        };

        ScreenLoader.showAsMainScreen(mainStage, "ChooseCards.fxml", "Choose Initial Cards - " + currentPlayer.getName(),
                (ChooseCardsController c) -> c.setup(currentPlayer, offer, CardSelectionContext.DRAFT, onConfirmAction));
    }

    public static void startGameWithChosenCards(GameState gameState) {
        var result = ScreenLoader.loadFxml("GameScreen.fxml");
        TerraformingMarsController mainController = (TerraformingMarsController) result.controller();
        Scene mainGameScene = ScreenLoader.createScene(result.root());

        mainController.setupGame(gameState);

        mainStage.setScene(mainGameScene);
        mainStage.setTitle("Terraforming Mars");
    }

    public static void startFinalGreeneryPhase(List<Player> players, TerraformingMarsController mainController) {
        ScreenLoader.showAsModal(mainStage, "FinalGreeneryScreen.fxml", "Final Greenery Conversion",
                0.4, 0.5, (FinalGreeneryController c) -> c.setup(players, mainController));
    }

    public static void showGameOverScreen(List<Player> rankedPlayers) {
        ScreenLoader.showAsModal(mainStage, "GameOver.fxml", "Game Over - Final Score",
                0.4, 0.5, (GameOverController c) -> c.setFinalScores(rankedPlayers));
    }
}