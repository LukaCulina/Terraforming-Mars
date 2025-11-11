package hr.terraforming.mars.terraformingmars.controller;

import hr.terraforming.mars.terraformingmars.enums.ActionType;
import hr.terraforming.mars.terraformingmars.replay.ReplayManager;
import hr.terraforming.mars.terraformingmars.thread.GetLastGameMoveThread;
import hr.terraforming.mars.terraformingmars.util.*;
import hr.terraforming.mars.terraformingmars.view.GameScreens;
import hr.terraforming.mars.terraformingmars.manager.*;
import hr.terraforming.mars.terraformingmars.model.*;
import hr.terraforming.mars.terraformingmars.service.SaveLoadService;
import hr.terraforming.mars.terraformingmars.view.HexBoardDrawer;
import hr.terraforming.mars.terraformingmars.view.components.ActionPanelComponents;
import hr.terraforming.mars.terraformingmars.view.components.GlobalStatusComponents;
import hr.terraforming.mars.terraformingmars.view.components.PlayerControlComponents;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.*;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Window;
import javafx.util.Duration;
import org.slf4j.*;

import java.io.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;

public class TerraformingMarsController {

    private static final Logger logger = LoggerFactory.getLogger(TerraformingMarsController.class);

    @FXML private AnchorPane hexBoardPane;
    @FXML private BorderPane gameBoardPane;
    @FXML private StackPane temperaturePane;
    @FXML private GridPane bottomGrid;
    @FXML private VBox currentPlayerBoardContainer;
    @FXML private HBox playerListBar;
    @FXML private VBox standardProjectsBox;
    @FXML private ProgressBar oxygenProgressBar;
    @FXML private Label oxygenLabel;
    @FXML private ProgressBar temperatureProgressBar;
    @FXML private Label temperatureLabel;
    @FXML private Label generationLabel;
    @FXML private Label phaseLabel;
    @FXML private Button passTurnButton;
    @FXML private Button convertHeatButton;
    @FXML private Button convertPlantsButton;
    @FXML private Label oceansLabel;
    @FXML private VBox milestonesBox;
    @FXML private Button cancelPlacementButton;
    @FXML private VBox playerInterface;
    @FXML private Label lastMoveLabel;

    private PlacementManager placementManager;
    private UIManager uiManager;
    private ActionManager actionManager;
    private GameManager gameManager;
    private GameBoard gameBoard;
    private PlayerBoardController currentPlayerBoardController;
    private Player viewedPlayer = null;
    private final SaveLoadService saveLoadService = new SaveLoadService();
    private ReplayManager replayManager;

    @FXML
    private void initialize() {
        loadPlayerBoard();
    }

    public GameManager getGameManager() { return gameManager; }
    public GameBoard getGameBoard() { return gameBoard; }


    private void loadPlayerBoard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/hr/terraforming/mars/terraformingmars/PlayerBoard.fxml"));
            Node boardNode = loader.load();
            currentPlayerBoardController = loader.getController();
            currentPlayerBoardContainer.getChildren().setAll(boardNode);
        } catch (IOException e) {
            logger.error("FATAL: Could not load PlayerBoard.fxml. The application cannot start correctly.", e);
        }
    }

    public void setupGame(GameState gameState) {
        this.gameManager = gameState.gameManager();
        this.gameBoard = gameState.gameBoard();
        initializeComponents();
        this.gameManager.startGame();
        this.viewedPlayer = this.gameManager.getCurrentPlayer();
        Platform.runLater(this::updateAllUI);
        startMoveHistory();
    }

    private void initializeComponents() {
        this.placementManager = new PlacementManager(this, this.gameManager, this.gameBoard);
        GameFlowManager gameFlowManager = new GameFlowManager(this, this.gameManager, this.gameBoard);
        this.actionManager = new ActionManager(this, this.gameManager, this.gameBoard, this.placementManager, gameFlowManager);
        this.placementManager.setActionHandler(this.actionManager);
        cancelPlacementButton.setOnAction(_ -> placementManager.cancelPlacement());
        HexBoardDrawer hexBoardDrawer = new HexBoardDrawer(hexBoardPane, this.gameBoard, this.placementManager);
        GlobalStatusComponents statusComps = new GlobalStatusComponents(oxygenProgressBar, oxygenLabel, temperatureProgressBar, temperatureLabel, oceansLabel, generationLabel, phaseLabel);
        ActionPanelComponents panelComps = new ActionPanelComponents(milestonesBox, standardProjectsBox);
        PlayerControlComponents controlComps = new PlayerControlComponents(playerListBar, passTurnButton, convertHeatButton, convertPlantsButton);
        this.uiManager = new UIManager(this.gameBoard, this.gameManager, this.actionManager, hexBoardDrawer, statusComps, panelComps, controlComps);
        this.uiManager.initializeUIComponents(this, gameBoardPane, playerInterface, bottomGrid, temperaturePane);
        this.gameBoard.setOnGlobalParametersChanged(this::updateAllUI);
        this.replayManager = new ReplayManager(this);
    }

    private void startMoveHistory() {
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(4), _ -> {
            if (lastMoveLabel != null && lastMoveLabel.getScene() != null) {
                Platform.runLater(new GetLastGameMoveThread(lastMoveLabel));
            }
        }));
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
    }

    public void updateAllUI() {
        if (uiManager == null || gameManager == null || viewedPlayer == null) {
            logger.warn("UI update skipped because a critical component is null.");
            return;
        }

        boolean isPlacing = (placementManager != null && placementManager.isPlacementMode());
        uiManager.updateGeneralUI(viewedPlayer, isPlacing);

        if (currentPlayerBoardController != null) {
            currentPlayerBoardController.setPlayer(viewedPlayer, this::handlePlayCard);
        }

        if (uiManager.getHexBoardDrawer() != null) {
            uiManager.getHexBoardDrawer().drawBoard();
        }
    }

    public void showPlayerBoard(Player player) {
        this.viewedPlayer = player;
        updateAllUI();
    }

    public Window getSceneWindow() {
        return hexBoardPane.getScene().getWindow();
    }

    private void handlePlayCard(Card card) {
        actionManager.handlePlayCard(card);
    }

    public void onFinalGreeneryPhaseComplete() {
        logger.info("Final greenery conversion phase is complete. Proceeding to calculate final scores.");
        List<Player> rankedPlayers = gameManager.calculateFinalScores();

        Platform.runLater(() -> GameScreens.showGameOverScreen(rankedPlayers));
    }

    public void enterPlacementModeForFinalGreenery(Player player, Runnable onCompleteCallback) {
        if (placementManager != null) {
            placementManager.enterPlacementModeForFinalGreenery(player, onCompleteCallback);
        }
    }

    public void openSellPatentsWindow() {
        Consumer<List<Card>> onSaleCompleteAction = soldCards -> {

            String details = soldCards.stream().map(Card::getName).reduce((a,b) -> a + "," + b).orElse("");
            GameMove showModal = new GameMove(
                    gameManager.getCurrentPlayer().getName(),
                    ActionType.OPEN_SELL_PATENTS_MODAL,
                    details,
                    java.time.LocalDateTime.now()
            );
            actionManager.recordAndSaveMove(showModal);

            GameMove move = new GameMove(
                    gameManager.getCurrentPlayer().getName(),
                    ActionType.SELL_PATENTS,
                    "Sold " + soldCards.size() + " card(s)",
                    java.time.LocalDateTime.now()
            );

            actionManager.recordAndSaveMove(move);

            actionManager.performAction();
        };

        ScreenLoader.showAsModal(
                hexBoardPane.getScene().getWindow(),
                "SellPatents.fxml",
                "Sell Patents",
                0.5, 0.7,
                (SellPatentsController c) -> c.initData(gameManager.getCurrentPlayer(), onSaleCompleteAction)
        );
    }

    public ActionManager getActionManager() {
        return actionManager;
    }

    public void setViewedPlayer(Player player) {
        this.viewedPlayer = player;
    }

    public void drawBoard() {
        updateAllUI();
    }

    public void setGameControlsEnabled(boolean isEnabled) {

        if (passTurnButton != null) passTurnButton.setDisable(!isEnabled);
        if (convertHeatButton != null) convertHeatButton.setDisable(!isEnabled);
        if (convertPlantsButton != null) convertPlantsButton.setDisable(!isEnabled);

        if (standardProjectsBox != null) standardProjectsBox.setDisable(!isEnabled);
        if (milestonesBox != null) milestonesBox.setDisable(!isEnabled);

        if (currentPlayerBoardController != null) {
            currentPlayerBoardController.setHandInteractionEnabled(isEnabled);
        }

        if (cancelPlacementButton != null) {
            cancelPlacementButton.setVisible(!isEnabled);
        }
    }

    public void startNewGame() {
        GameMoveUtils.deleteMoveHistoryFile();
        XmlUtils.clearGameMoves();
        GameScreens.showChoosePlayersScreen();
    }

    public void saveGame() {
        saveLoadService.saveGame(gameManager, gameBoard);
    }

    public void loadGame() {
        GameState loadedState = saveLoadService.loadGame();
        if (loadedState != null) {
            this.gameManager = loadedState.gameManager();
            this.gameBoard = loadedState.gameBoard();
            this.gameManager.relink(this.gameBoard);
            initializeComponents();
            this.viewedPlayer = this.gameManager.getCurrentPlayer();
            updateAllUI();
            DialogUtils.showSuccessDialog("The game has been successfully loaded!");
        }
    }

    public void generateHtmlDocumentation() { DocumentationUtils.generateDocumentation(); }

    @FXML
    private void replayGame() {
        replayManager.startReplay();
    }

    public void prepareForReplay() {
        gameManager.getPlayers().forEach(Player::resetForNewGame);

        GameBoard newReplayBoard = new GameBoard();

        newReplayBoard.setOnGlobalParametersChanged(this::updateAllUI);

        this.gameBoard = newReplayBoard;
        this.gameManager.resetForNewGame(newReplayBoard);
        this.uiManager.linkNewGameBoard(newReplayBoard);

        this.viewedPlayer = this.gameManager.getCurrentPlayer();
        updateAllUI();
    }

    public void updateLastMoveLabel(GameMove lastGameMove) {
        if (lastGameMove != null) {
            StringBuilder sb = new StringBuilder("Last Move: ");
            sb.append(lastGameMove.getPlayerName()).append(" - ");
            sb.append(lastGameMove.getActionType()).append(" (").append(lastGameMove.getDetails()).append(")");
            if (lastGameMove.getRow() != null) {
                sb.append(" at (").append(lastGameMove.getRow()).append(", ").append(lastGameMove.getCol()).append(")");
            }
            sb.append(" at ").append(lastGameMove.getTimestamp().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
            lastMoveLabel.setText(sb.toString());
        } else {
            lastMoveLabel.setText("No moves recorded yet.");
        }
    }
}