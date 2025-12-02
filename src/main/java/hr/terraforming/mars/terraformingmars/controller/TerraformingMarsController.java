package hr.terraforming.mars.terraformingmars.controller;

import hr.terraforming.mars.terraformingmars.coordinator.ClientResearchCoordinator;
import hr.terraforming.mars.terraformingmars.coordinator.PlacementCoordinator;
import hr.terraforming.mars.terraformingmars.enums.GamePhase;
import hr.terraforming.mars.terraformingmars.enums.PlayerType;
import hr.terraforming.mars.terraformingmars.network.GameClientThread;
import hr.terraforming.mars.terraformingmars.network.GameServerThread;
import hr.terraforming.mars.terraformingmars.network.NetworkBroadcaster;
import hr.terraforming.mars.terraformingmars.replay.ReplayManager;
import hr.terraforming.mars.terraformingmars.service.GameStateService;
import hr.terraforming.mars.terraformingmars.ui.PlayerBoardLoader;
import hr.terraforming.mars.terraformingmars.ui.ResizeHandler;
import hr.terraforming.mars.terraformingmars.ui.UIInitializer;
import hr.terraforming.mars.terraformingmars.util.*;
import hr.terraforming.mars.terraformingmars.view.GameScreens;
import hr.terraforming.mars.terraformingmars.manager.*;
import hr.terraforming.mars.terraformingmars.model.*;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.*;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Window;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class TerraformingMarsController {

    @Getter
    @FXML private AnchorPane hexBoardPane;
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
    @FXML private Button cancelPlacementButton;
    @FXML public BorderPane playerInterface;
    @FXML private Label lastMoveLabel;
    @FXML private VBox chatBoxContainer;

    @Getter private PlacementManager placementManager;
    @Getter private UIManager uiManager;
    @Getter private ActionManager actionManager;
    @Getter public GameManager gameManager;
    @Setter @Getter private GameBoard gameBoard;
    private ChatManager chatManager;
    private PlayerBoardController currentPlayerBoardController;
    @Getter private PlacementCoordinator placementCoordinator;
    private ClientResearchCoordinator clientResearchCoordinator;
    @Setter private Player viewedPlayer = null;
    private final GameStateService gameStateService = new GameStateService();
    private ReplayManager replayManager;
    private Timeline moveHistoryTimeline;
    @FXML private ListView<String> chatListView;
    @FXML private TextField chatInput;
    private final AtomicInteger localStateCallCount = new AtomicInteger(0);

    @FXML
    private void initialize() {
        currentPlayerBoardController = PlayerBoardLoader.loadPlayerBoard(currentPlayerBoardContainer);
    }

    private void updatePlayerHighlight(Player currentPlayer) {
        for (Node node : playerListBar.getChildren()) {
            node.getStyleClass().remove("current-player-highlight");

            Object userData = node.getUserData();
            if (userData != null && userData.equals(currentPlayer.getName())) {
                node.getStyleClass().add("current-player-highlight");
            }
        }
    }

    public void updateFromNetwork(GameState state) {
        if (!validateGameState(state)) {
            return;
        }

        logNetworkUpdate(state);
        updateLocalState(state);
        updateViewedPlayer();
        updateGameControls(state);

        if (clientResearchCoordinator != null) {
            clientResearchCoordinator.checkAndHandle();
        }

        updateAllUI();
        updatePlayerHighlight(gameManager.getCurrentPlayer());
    }

    private boolean validateGameState(GameState state) {
        if (state == null) {
            log.error("Received null GameState!");
            return false;
        }
        if (state.gameManager() == null || state.gameBoard() == null) {
            log.error("Critical error: Received incomplete GameState! Manager={}, Board={}",
                    state.gameManager(), state.gameBoard());
            return false;
        }
        return true;
    }

    private void logNetworkUpdate(GameState state) {
        log.debug("🔄 NetUpdate: Gen={}, Phase={}, CurrentTurn={}, MyName={}",
                state.gameManager().getGeneration(),
                state.gameManager().getCurrentPhase(),
                state.gameManager().getCurrentPlayer().getName(),
                ApplicationConfiguration.getInstance().getMyPlayerName());
    }

    private void updateLocalState(GameState state) {
        int callId = localStateCallCount.incrementAndGet();
        log.info("🎯 updateLocalState() CALL #{} from {}", callId,
                Thread.currentThread().getStackTrace()[2]);

        this.gameManager = state.gameManager();
        this.gameBoard = state.gameBoard();

        gameManager.relink(gameBoard);

        if (uiManager != null) {
            uiManager.updateGameState(this.gameManager, this.gameBoard);
        }
        if (actionManager != null) {
            actionManager.updateState(this.gameManager, this.gameBoard);
        }
    }

    private void updateViewedPlayer() {
        String myPlayerName = ApplicationConfiguration.getInstance().getMyPlayerName();

        if (myPlayerName != null) {
            Player myPlayer = gameManager.getPlayerByName(myPlayerName);
            if (myPlayer != null) {
                this.viewedPlayer = myPlayer;
                return;
            }
        }
        this.viewedPlayer = gameManager.getCurrentPlayer();
    }

    private void updateGameControls(GameState state) {
        String myPlayerName = ApplicationConfiguration.getInstance().getMyPlayerName();
        String currentPlayerName = state.gameManager().getCurrentPlayer().getName();

        boolean isMyTurn = currentPlayerName.equals(myPlayerName);
        boolean isActionPhase = gameManager.getCurrentPhase() == GamePhase.ACTIONS;

        if (isMyTurn && isActionPhase) {
            setGameControlsEnabled(true);
            log.debug("✅ Controls ENABLED for {}", myPlayerName);
            Platform.runLater(this::updateAllUI);
        } else {
            setGameControlsEnabled(false);
            log.debug("🚫 Controls DISABLED (MyTurn: {}, ActionPhase: {})", isMyTurn, isActionPhase);
        }
    }

    public void onLocalPlayerMove(GameMove move) {
        PlayerType playerType = ApplicationConfiguration.getInstance().getPlayerType();

        if (playerType == PlayerType.HOST) {
            NetworkBroadcaster broadcaster = ApplicationConfiguration.getInstance().getBroadcaster();
            if (broadcaster != null) {
                broadcaster.broadcast();
            }
        } else if (playerType == PlayerType.CLIENT) {
            GameClientThread client = ApplicationConfiguration.getInstance().getGameClient();
            if (client != null) {
                client.sendMove(move);
            }
        }
    }

    @FXML
    private void sendChatMessage() {
        if (chatManager != null) {
            chatManager.sendMessage();
        }
    }

    public void setupGame(GameState gameState) {
        log.info("Setting up game - gameBoard = {}", gameState.gameBoard() != null ? "NOT NULL" : "NULL");

        this.gameManager = gameState.gameManager();
        this.gameBoard = gameState.gameBoard();

        if (gameBoard == null) {
            log.error("GameBoard is null - cannot initialize!");
            return;
        }

        if (gameManager != null) {
            gameManager.relink(gameBoard);
        }

        initializeComponents();

        PlayerType playerType = ApplicationConfiguration.getInstance().getPlayerType();
        if (chatManager != null) {
            chatManager.setupChatSystem(playerType);
        }

        setupClientNetworkListeners(playerType);

        setupInitialPlayerView();

        if (playerType == PlayerType.LOCAL || playerType == PlayerType.HOST) {
            this.gameManager.startGame();
        }

        finalizeUISetup();
    }

    private void setupClientNetworkListeners(PlayerType playerType) {
        if (playerType == PlayerType.CLIENT) {
            setGameControlsEnabled(false);
            log.info("🚫 CLIENT controls disabled on setup (waiting for host turn info)");

            GameClientThread client = ApplicationConfiguration.getInstance().getGameClient();
            if (client != null) {
                client.addGameStateListener(this::updateFromNetwork);
                log.info("CLIENT registered UI update listener");
            }
        }
    }

    private void setupInitialPlayerView() {
        String myPlayerName = ApplicationConfiguration.getInstance().getMyPlayerName();
        Player playerToShow = gameManager.getCurrentPlayer();

        if (myPlayerName != null) {
            Player myPlayer = gameManager.getPlayerByName(myPlayerName);
            if (myPlayer != null) {
                playerToShow = myPlayer;
            }
        }
        showPlayerBoard(playerToShow);
    }

    private void finalizeUISetup() {
        Platform.runLater(() -> gameBoardPane.maxHeightProperty().bind(
                hexBoardPane.getScene().heightProperty().subtract(28)
        ));
        Platform.runLater(this::updateAllUI);
        startMoveHistory();
        updatePlayerHighlight(gameManager.getCurrentPlayer());
    }

    public void initializeComponents() {
        GameFlowManager gameFlowManager = new GameFlowManager(this, this.gameManager, this.gameBoard);
        this.actionManager = new ActionManager(this, this.gameManager, this.gameBoard, gameFlowManager);
        PlayerType playerType = ApplicationConfiguration.getInstance().getPlayerType();
        if (playerType == PlayerType.HOST) {
            GameServerThread server = ApplicationConfiguration.getInstance().getGameServer();
            if (server != null) {
                server.setActionManager(this.actionManager);
                log.info("✅ ActionManager injected into server and all clients");
            } else {
                log.warn("⚠️ GameServerThread is null, cannot inject ActionManager!");
            }
        }
        this.placementManager = new PlacementManager(this, this.gameManager, this.gameBoard, this.actionManager);
        this.placementCoordinator = new PlacementCoordinator(this.placementManager);
        this.chatManager = new ChatManager(chatListView, chatInput, chatBoxContainer);
        this.clientResearchCoordinator = new ClientResearchCoordinator(this);
        cancelPlacementButton.setOnAction(_ -> placementManager.cancelPlacement());

        this.uiManager = UIInitializer.initUI(this, gameBoard, gameManager, actionManager);

        ResizeHandler.attachResizeListeners(hexBoardPane, uiManager.getHexBoardDrawer());

        this.gameBoard.setOnGlobalParametersChanged(this::updateAllUI);
        this.replayManager = new ReplayManager(this);
    }

    public void startMoveHistory() {
        if (moveHistoryTimeline == null) {
            moveHistoryTimeline = GameMoveUtils.createLastMoveTimeline(lastMoveLabel);
            moveHistoryTimeline.play();
        }
    }

    public void updateAllUI() {
        if (uiManager == null || gameManager == null || viewedPlayer == null) return;

        String myName = ApplicationConfiguration.getInstance().getMyPlayerName();
        boolean isMyTurn = gameManager.getCurrentPlayer().getName().equals(myName);
        if (ApplicationConfiguration.getInstance().getPlayerType() == PlayerType.LOCAL) {
            isMyTurn = true;
        }

        boolean isPlacing = (placementManager != null && placementManager.isPlacementMode());
        uiManager.updateGeneralUI(viewedPlayer, isPlacing, isMyTurn);

        if (currentPlayerBoardController != null) {
            currentPlayerBoardController.setPlayer(viewedPlayer, actionManager);
        }

        uiManager.getHexBoardDrawer().drawBoard();
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

    public void setGameControlsEnabled(boolean isEnabled) {

        List.of(passTurnButton, convertHeatButton, convertPlantsButton,
                        standardProjectsBox, milestonesBox)
                .forEach(node -> { if (node != null) node.setDisable(!isEnabled); });

        if (currentPlayerBoardController != null) {
            currentPlayerBoardController.setHandInteractionEnabled(isEnabled);
        }
    }

    public void setCancelButtonVisible(boolean visible) {
        if (cancelPlacementButton != null) {
            cancelPlacementButton.setVisible(visible);
        }
    }

    public void startNewGame() {
        gameStateService.clearGameData();
        GameScreens.showChooseModeScreen();
    }

    public void saveGame() {
        gameStateService.saveGame(gameManager, gameBoard);
    }

    public void loadGame() {
        GameState loadedState = gameStateService.loadGame();
        if (loadedState != null) {
            this.gameManager = loadedState.gameManager();
            this.gameBoard = loadedState.gameBoard();
            this.gameManager.relink(this.gameBoard);
            initializeComponents();
            this.viewedPlayer = this.gameManager.getCurrentPlayer();
            updateAllUI();
            DialogUtils.showDialog("The game has been successfully loaded!");
        }
    }

    public void generateHtmlDocumentation() { DocumentationUtils.generateDocumentation(); }

    @FXML
    private void replayGame() {
        replayManager.startReplay();
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