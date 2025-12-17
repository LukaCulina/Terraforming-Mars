package hr.terraforming.mars.terraformingmars.controller.game;

import hr.terraforming.mars.terraformingmars.coordinator.*;
import hr.terraforming.mars.terraformingmars.replay.ReplayManager;
import hr.terraforming.mars.terraformingmars.service.GameStateService;
import hr.terraforming.mars.terraformingmars.ui.PlayerBoardLoader;
import hr.terraforming.mars.terraformingmars.util.*;
import hr.terraforming.mars.terraformingmars.view.ScreenNavigator;
import hr.terraforming.mars.terraformingmars.manager.*;
import hr.terraforming.mars.terraformingmars.model.*;
import javafx.animation.*;
import javafx.fxml.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Window;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.time.format.DateTimeFormatter;

@Slf4j
public class GameScreenController {

    @Getter
    @FXML private AnchorPane hexBoardPane;
    @Getter
    @FXML public BorderPane gameBoardPane;
    @FXML public StackPane temperaturePane;
    @FXML public GridPane bottomGrid;
    @FXML private VBox currentPlayerBoardContainer;
    @FXML public HBox playerListBar;
    @FXML public VBox standardProjectsBox;
    @FXML public ProgressBar oxygenProgressBar;
    @FXML public Label oxygenLabel;
    @FXML public ProgressBar temperatureProgressBar;
    @FXML public Label temperatureLabel;
    @FXML public Label generationLabel;
    @FXML public Label phaseLabel;
    @FXML public Button passTurnButton;
    @FXML public Button convertHeatButton;
    @FXML public Button convertPlantsButton;
    @FXML public Label oceansLabel;
    @FXML public VBox milestonesBox;
    @Getter
    @FXML private Button cancelPlacementButton;
    @FXML public BorderPane playerInterface;
    @FXML private Label lastMoveLabel;
    @Getter
    @FXML private VBox chatBoxContainer;
    @Getter
    @FXML private ListView<String> chatListView;
    @Getter
    @FXML private TextField chatInput;

    @Setter @Getter private PlacementManager placementManager;
    @Setter @Getter private GameScreenManager gameScreenManager;
    @Setter @Getter private ActionManager actionManager;
    @Setter @Getter public GameManager gameManager;
    @Setter @Getter private GameBoard gameBoard;
    @Setter @Getter private ChatManager chatManager;
    @Getter private PlayerBoardController currentPlayerBoardController;
    @Getter private NetworkCoordinator networkCoordinator;
    @Getter private GameSetupCoordinator setupCoordinator;
    private GameScreenCoordinator gameScreenCoordinator;
    @Setter private Player viewedPlayer = null;
    private final GameStateService gameStateService = new GameStateService();
    @Setter
    private ReplayManager replayManager;
    @Getter private Timeline moveHistoryTimeline;

    @FXML
    private void initialize() {
        currentPlayerBoardController = PlayerBoardLoader.load(currentPlayerBoardContainer);
        this.networkCoordinator = new NetworkCoordinator(this);
        this.setupCoordinator = new GameSetupCoordinator(this);
        this.gameScreenCoordinator = new GameScreenCoordinator();
        addDebugButtons();

    }

    private void addDebugButtons() {
        HBox debugBox = new HBox(5);
        debugBox.setStyle("-fx-padding: 5;");

        Button triggerFinalBtn = new Button("🎯 Trigger Final Gen");
        triggerFinalBtn.setOnAction(_ -> debugTriggerFinalGeneration());

        debugBox.getChildren().add(triggerFinalBtn);

        // Dodaj u bottomGrid ili playerInterface
        if (bottomGrid != null) {
            bottomGrid.add(debugBox, 0, bottomGrid.getRowCount());
        }
    }


    @FXML
    private void debugTriggerFinalGeneration() {
        if (gameBoard != null) {
            gameBoard.setTemperature(GameBoard.MAX_TEMPERATURE);
            gameBoard.setOxygenLevel(GameBoard.MAX_OXYGEN);
            gameBoard.setOceansPlaced(GameBoard.MAX_OCEANS);
            log.info("🎯 DEBUG: All parameters set to MAX - Final Generation triggered!");
            updateAllUI();
            broadcastIfHost();
        }
    }

    private void broadcastIfHost() {
        var config = ApplicationConfiguration.getInstance();
        if (config.getPlayerType() == hr.terraforming.mars.terraformingmars.enums.PlayerType.HOST) {
            var broadcaster = config.getBroadcaster();
            if (broadcaster != null) {
                broadcaster.broadcast();
            }
        }
    }

    public void setupGame(GameState gameState) {
        setupCoordinator.setupNewGame(gameState);
    }

    public void updateFromNetwork(GameState state) {
        networkCoordinator.handleNetworkUpdate(state);
    }

    public void onLocalPlayerMove(GameMove move) {
        networkCoordinator.broadcastMove(move);
    }

    public void startMoveHistory() {
        if (moveHistoryTimeline == null) {
            moveHistoryTimeline = GameMoveUtils.createLastMoveTimeline(lastMoveLabel);
            moveHistoryTimeline.play();
        }
    }

    public void updateAllUI() {
        gameScreenCoordinator.updateAllUI(viewedPlayer, gameManager, placementManager,
                currentPlayerBoardController, actionManager, gameScreenManager);
    }

    public void updatePlayerHighlightForCurrentPlayer() {
        if (gameManager != null && gameManager.getCurrentPlayer() != null) {
            gameScreenCoordinator.updatePlayerHighlight(gameManager.getCurrentPlayer(), playerListBar);
        }
    }

    public void setGameControlsEnabled(boolean isEnabled) {
        gameScreenCoordinator.setGameControlsEnabled(isEnabled, currentPlayerBoardController,
                passTurnButton, convertHeatButton, convertPlantsButton,
                standardProjectsBox, milestonesBox);
    }

    public void showPlayerBoard(Player player) {
        this.viewedPlayer = player;
        updateAllUI();
    }

    public Window getSceneWindow() {
        return hexBoardPane.getScene().getWindow();
    }

    public void drawBoard() {
        updateAllUI();
    }

    public void setCancelButtonVisible(boolean visible) {
        if (cancelPlacementButton != null) {
            cancelPlacementButton.setVisible(visible);
        }
    }

    @FXML
    private void sendChatMessage() {
        if (chatManager != null) {
            chatManager.sendMessage();
        }
    }

    @FXML
    private void replayGame() {
        replayManager.startReplay();
    }

    public void startNewGame() {
        GameSessionManager.resetForNewGame();
        gameStateService.clearGameData();
        ScreenNavigator.showChooseModeScreen();
    }

    public void saveGame() {
        gameStateService.saveGame(gameManager, gameBoard);
    }

    public void loadGame() {
        GameState loadedState = gameStateService.loadGame();
        if (loadedState != null) {
            setupCoordinator.setupLoadedGame(loadedState);
            this.viewedPlayer = this.gameManager.getCurrentPlayer();
            updateAllUI();
            DialogUtils.showDialog("The game has been successfully loaded!");
        }
    }

    public void generateHtmlDocumentation() {
        DocumentationUtils.generateDocumentation();
    }

    public void updateLastMoveLabel(GameMove lastGameMove) {
        if (lastGameMove != null) {
            StringBuilder sb = new StringBuilder("Last Move: ");
            sb.append(lastGameMove.playerName()).append(" - ");
            sb.append(lastGameMove.actionType()).append(" (").append(lastGameMove.details()).append(")");
            if (lastGameMove.row() != null) {
                sb.append(" at (").append(lastGameMove.row()).append(", ").append(lastGameMove.col()).append(")");
            }
            sb.append(" at ").append(lastGameMove.timestamp().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
            lastMoveLabel.setText(sb.toString());
        } else {
            lastMoveLabel.setText("No moves recorded yet.");
        }
    }
}