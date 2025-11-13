package hr.terraforming.mars.terraformingmars.view;

import hr.terraforming.mars.terraformingmars.controller.*;
import hr.terraforming.mars.terraformingmars.enums.ActionType;
import hr.terraforming.mars.terraformingmars.model.*;
import hr.terraforming.mars.terraformingmars.util.ScreenLoader;
import hr.terraforming.mars.terraformingmars.util.XmlUtils;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Consumer;

@Slf4j
public class GameScreens {

    private GameScreens() { throw new IllegalStateException("Utility class"); }

    @Setter
    private static Stage mainStage;

    public static void showStartMenu() {
        ScreenLoader.showAsMainScreen(
                mainStage,
                "StartMenu.fxml",
                "Terraforming Mars - Main Menu",
                (StartMenuController _) -> {}
        );

        log.info("Main Menu displayed.");
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

            int cost = chosenCards.size() * 3;

            currentPlayer.spendMC(cost);

            currentPlayer.getHand().addAll(chosenCards);

            if (gameManager.advanceDraftPlayer()) {
                showInitialCardDraftScreen(gameManager);
            } else {
                createAndSaveInitialSetupMove(gameManager);

                gameManager.startGame();
                GameState gameState = new GameState(gameManager, gameManager.getGameBoard());
                startGameWithChosenCards(gameState);
            }
        };

        ScreenLoader.showAsMainScreen(mainStage, "ChooseCards.fxml", "Choose Initial Cards - " + currentPlayer.getName(),
                (ChooseCardsController c) -> c.setup(currentPlayer, offer, onConfirmAction));
    }

    public static void startGameWithChosenCards(GameState gameState) {
        var result = ScreenLoader.loadFxml("GameScreen.fxml");
        TerraformingMarsController mainController = (TerraformingMarsController) result.controller();
        Scene mainGameScene = ScreenLoader.createScene(result.root());

        mainController.setupGame(gameState);

        mainStage.setScene(mainGameScene);
        mainStage.setTitle("Terraforming Mars");
    }

    public static void showGameScreen(GameState gameState) {
        var result = ScreenLoader.loadFxml("GameScreen.fxml");
        TerraformingMarsController mainController = (TerraformingMarsController) result.controller();
        Scene mainGameScene = ScreenLoader.createScene(result.root());

        mainController.setupGame(gameState);

        mainStage.setScene(mainGameScene);
        mainStage.setTitle("Terraforming Mars - Loaded Game");

        log.info("Loaded game displayed on main screen.");
    }

    public static void startFinalGreeneryPhase(GameManager gameManager, TerraformingMarsController mainController) {
        ScreenLoader.showAsModal(mainStage, "FinalGreeneryScreen.fxml", "Final Greenery Conversion",
                0.4, 0.5, (FinalGreeneryController c) -> c.setup(gameManager, mainController));
    }

    public static void showGameOverScreen(List<Player> rankedPlayers) {
        ScreenLoader.showAsModal(mainStage, "GameOver.fxml", "Game Over - Final Score",
                0.4, 0.5, (GameOverController c) -> c.setFinalScores(rankedPlayers));
    }

    private static void createAndSaveInitialSetupMove(GameManager gameManager) {
        try {
            Map<String, Object> setupData = new HashMap<>();
            for (Player player : gameManager.getPlayers()) {
                Map<String, Object> playerData = new HashMap<>();
                if (player.getCorporation() != null) {
                    playerData.put("corporation", player.getCorporation().name());
                } else {
                    playerData.put("corporation", "N/A");
                }
                playerData.put("hand", player.getHand().stream().map(Card::getName).toList());
                setupData.put(player.getName(), playerData);
            }

            String jsonDetails = new com.google.gson.Gson().toJson(setupData);

            GameMove initialMove = new GameMove(
                    "System",
                    ActionType.INITIAL_SETUP,
                    jsonDetails,
                    LocalDateTime.now()
            );

            XmlUtils.appendGameMove(initialMove);
            log.debug("INITIAL_SETUP move successfully saved to XML!");

        } catch (Exception e) {
            log.error("Fatal error occurred during initial state saving.");
            new Alert(Alert.AlertType.ERROR, "Error saving initial state for replay. See console for details.\n\n" + e.getMessage()).showAndWait();
        }
    }
}