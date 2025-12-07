package hr.terraforming.mars.terraformingmars.view;

import hr.terraforming.mars.terraformingmars.controller.game.*;
import hr.terraforming.mars.terraformingmars.controller.setup.*;
import hr.terraforming.mars.terraformingmars.model.*;
import hr.terraforming.mars.terraformingmars.util.GameMoveUtils;
import hr.terraforming.mars.terraformingmars.util.ScreenLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import java.util.*;
import java.util.function.Consumer;

@Slf4j
public class ScreenNavigator {

    private ScreenNavigator() {
        throw new IllegalStateException("Utility class");
    }

    @Getter
    @Setter
    private static Stage mainStage;
    private static final int STARTING_CARDS = 6;
    private static final int CARD_COST = 3;
    private static final double WIDTH_PERCENTAGE = 0.4;
    private static final double HEIGHT_PERCENTAGE = 0.5;

    public static void showStartMenu() {
        ScreenLoader.showAsMainScreen(
                mainStage,
                "StartMenu.fxml",
                "Terraforming Mars - Main Menu",
                (StartMenuController _) -> {}
        );

        log.info("Main Menu displayed.");
    }

    public static void showChooseModeScreen() {
        ScreenLoader.showAsMainScreen(
                mainStage,
                "ChooseMode.fxml",
                "Choose Mode",
                (ChooseModeController _) -> {}
        );
    }

    public static void showChooseOnlineModeScreen() {
        ScreenLoader.showAsMainScreen(
                mainStage,
                "ChooseOnlineMode.fxml",
                "Choose Online Mode",
                (ChooseOnlineModeController _) -> {}
        );
    }

    public static void showJoinGameScreen() {
        ScreenLoader.showAsMainScreen(
                mainStage,
                "JoinGame.fxml",
                "Join Game",
                (JoinGameController _) -> {}
        );
    }

    public static void showChooseNameScreen(GameManager gameManager, GameBoard gameBoard) {
        ScreenLoader.showAsMainScreen(
                mainStage,
                "ChooseName.fxml",
                "Choose a name",
                (ChooseNameController controller) -> controller.setup(gameManager, gameBoard)
        );
    }

    public static void showWaitingForPlayersScreen(GameManager gameManager, int expectedPlayerCount) {
        ScreenLoader.showAsMainScreen(
                mainStage,
                "WaitingScreen.fxml",
                "Waiting for Players",
                (WaitingScreenController controller) -> controller.setup(gameManager, expectedPlayerCount)
        );
    }

    public static void showChoosePlayersScreen() {
        ScreenLoader.showAsMainScreen(mainStage, "ChoosePlayers.fxml", "Choose the number of players", (ChoosePlayersController _) -> {});
    }

    public static void showChooseCorporationScreen(GameManager gameManager) {
        Player currentPlayer = gameManager.getCurrentPlayer();
        List<Corporation> offer = gameManager.getCorporationOffer();
        showChooseCorporationScreen(currentPlayer, offer, gameManager);
    }

    public static void showChooseCorporationScreen(Player player, List<Corporation> offer, GameManager gameManager) {
        ScreenLoader.showAsMainScreen(
                mainStage,
                "ChooseCorporation.fxml",
                "Choose for " + player.getName(),
                (ChooseCorporationController c) -> c.setCorporationOptions(player, offer, gameManager)
        );
    }

    public static void showInitialCardDraftScreen(GameManager gameManager) {
        Player currentPlayer = gameManager.getCurrentPlayerForDraft();
        List<Card> offer = gameManager.drawCards(STARTING_CARDS);
        showInitialCardDraftScreen(currentPlayer, offer, gameManager);
    }

    public static void showInitialCardDraftScreen(Player player, List<Card> offer, GameManager gameManager) {
        Consumer<List<Card>> onConfirmAction =
                chosenCards -> handleCardDraftConfirmation(chosenCards, player, gameManager);

        ScreenLoader.showAsMainScreen(
                mainStage,
                "ChooseCards.fxml",
                "Choose Initial Cards - " + player.getName(),
                (ChooseCardsController c) -> c.setup(player, offer, onConfirmAction, gameManager, false)
        );
    }

    private static void handleCardDraftConfirmation(
            List<Card> chosenCards,
            Player currentPlayer,
            GameManager gameManager) {

        int cost = chosenCards.size() * CARD_COST;
        currentPlayer.spendMC(cost);
        currentPlayer.getHand().addAll(chosenCards);

        if (gameManager.advanceDraftPlayer()) {
            showInitialCardDraftScreen(gameManager);
        } else {
            GameMoveUtils.saveInitialSetupMove(gameManager);
            gameManager.startGame();
            GameState gameState = new GameState(gameManager, gameManager.getGameBoard());
            startGameWithChosenCards(gameState);
        }
    }

    public static void startGameWithChosenCards(GameState gameState) {
        var result = ScreenLoader.loadFxml("GameScreen.fxml");
        TerraformingMarsController mainController = (TerraformingMarsController) result.controller();
        Scene mainGameScene = ScreenLoader.createScene(result.root());
        ApplicationConfiguration.getInstance().setActiveGameController(mainController);

        mainController.setupGame(gameState);

        mainStage.setScene(mainGameScene);
        mainStage.setTitle("Terraforming Mars");
    }

    public static void startFinalGreeneryPhase(GameManager gameManager, TerraformingMarsController mainController) {
        ScreenLoader.showAsModal(mainStage, "FinalGreeneryScreen.fxml", "Final Greenery Conversion",
                WIDTH_PERCENTAGE, HEIGHT_PERCENTAGE, (FinalGreeneryController c) -> c.setup(gameManager, mainController));
    }

    public static void showGameOverScreen(List<Player> rankedPlayers) {
        ScreenLoader.showAsModal(mainStage, "GameOver.fxml", "Game Over - Final Score",
                WIDTH_PERCENTAGE, HEIGHT_PERCENTAGE, (GameOverController c) -> c.setFinalScores(rankedPlayers));
    }
}