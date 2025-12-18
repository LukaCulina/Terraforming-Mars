package hr.terraforming.mars.terraformingmars.view;

import hr.terraforming.mars.terraformingmars.controller.game.*;
import hr.terraforming.mars.terraformingmars.controller.setup.*;
import hr.terraforming.mars.terraformingmars.model.*;
import hr.terraforming.mars.terraformingmars.util.GameMoveUtils;
import hr.terraforming.mars.terraformingmars.util.ScreenUtils;
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

    @Getter @Setter
    private static Stage mainStage;
    private static final int STARTING_CARDS = 6;
    private static final int CARD_COST = 3;

    public static void showStartMenu() {
        ScreenUtils.showAsMainScreen(
                mainStage,
                "StartMenu.fxml",
                "Terraforming Mars - Main Menu",
                (StartMenuController _) -> {}
        );

        log.info("Main Menu displayed.");
    }

    public static void showChooseModeScreen() {
        ScreenUtils.showAsMainScreen(
                mainStage,
                "ChooseMode.fxml",
                "Choose Mode",
                (ChooseModeController _) -> {}
        );
    }

    public static void showChooseOnlineModeScreen() {
        ScreenUtils.showAsMainScreen(
                mainStage,
                "ChooseOnlineMode.fxml",
                "Choose Online Mode",
                (ChooseOnlineModeController _) -> {}
        );
    }

    public static void showJoinGameScreen() {
        ScreenUtils.showAsMainScreen(
                mainStage,
                "JoinGame.fxml",
                "Join Game",
                (JoinGameController _) -> {}
        );
    }

    public static void showChooseNameScreen(GameManager gameManager, GameBoard gameBoard) {
        ScreenUtils.showAsMainScreen(
                mainStage,
                "ChooseName.fxml",
                "Choose a name",
                (ChooseNameController controller) -> controller.setup(gameManager, gameBoard)
        );
    }

    public static void showWaitingForPlayersScreen(GameManager gameManager, int expectedPlayerCount) {
        ScreenUtils.showAsMainScreen(
                mainStage,
                "WaitingScreen.fxml",
                "Waiting for Players",
                (WaitingScreenController controller) -> controller.setup(gameManager, expectedPlayerCount)
        );
    }

    public static void showChoosePlayersScreen() {
        ScreenUtils.showAsMainScreen(mainStage, "ChoosePlayers.fxml", "Choose the number of players", (ChoosePlayersController _) -> {});
    }

    public static void showChooseCorporationScreen(GameManager gameManager) {
        Player currentPlayer = gameManager.getCurrentPlayer();
        List<Corporation> offer = gameManager.getCorporationOffer();
        showChooseCorporationScreen(currentPlayer, offer, gameManager);
    }

    public static void showChooseCorporationScreen(Player player, List<Corporation> offer, GameManager gameManager) {
        ScreenUtils.showAsMainScreen(
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

        ScreenUtils.showAsMainScreen(
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
        var result = ScreenUtils.loadFxml("GameScreen.fxml");
        GameScreenController mainController = (GameScreenController) result.controller();
        Scene mainGameScene = ScreenUtils.createScene(result.root());
        ApplicationConfiguration.getInstance().setActiveGameController(mainController);

        mainController.setupGame(gameState);

        mainStage.setScene(mainGameScene);
        mainStage.setTitle("Terraforming Mars");
    }

    public static void showGameOverScreen(List<Player> rankedPlayers) {
        ScreenUtils.showAsMainScreen(
                mainStage,
                "GameOver.fxml",
                "Game Over - Final Score",
                 (GameOverController c) -> c.setFinalScores(rankedPlayers));
    }
}