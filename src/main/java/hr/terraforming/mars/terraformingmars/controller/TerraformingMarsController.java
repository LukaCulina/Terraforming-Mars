package hr.terraforming.mars.terraformingmars.controller;

import hr.terraforming.mars.terraformingmars.chat.ChatService;
import hr.terraforming.mars.terraformingmars.enums.PlayerType;
import hr.terraforming.mars.terraformingmars.jndi.ConfigurationKey;
import hr.terraforming.mars.terraformingmars.jndi.ConfigurationReader;
import hr.terraforming.mars.terraformingmars.network.GameClientThread;
import hr.terraforming.mars.terraformingmars.network.GameServerThread;
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
import javafx.util.Duration;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.time.format.DateTimeFormatter;
import java.util.List;

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
    @FXML public VBox playerInterface;
    @FXML private Label lastMoveLabel;

    @Getter private PlacementManager placementManager;
    @Getter private UIManager uiManager;
    @Getter private ActionManager actionManager;
    @Getter public GameManager gameManager;
    @Setter @Getter private GameBoard gameBoard;
    private PlayerBoardController currentPlayerBoardController;
    @Getter private PlacementCoordinator placementCoordinator;
    @Setter private Player viewedPlayer = null;
    private final GameStateService gameStateService = new GameStateService();
    private ReplayManager replayManager;
    private Timeline moveHistoryTimeline;
    private ChatService chatService;
    @FXML private ListView<String> chatListView;
    @FXML private TextField chatInput;

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

    private void updateFromNetwork(GameState state) {

        if (state == null) {
            log.error("Received null GameState!");
            return;
        }

        boolean gameBoardIsNull = (state.gameBoard() == null);
        boolean gameManagerIsNull = (state.gameManager() == null);

        log.info("Received GameState update - gameManager: {}, gameBoard: {}",
                gameManagerIsNull ? "NULL" : "NOT NULL",
                gameBoardIsNull ? "NULL" : "NOT NULL");

        if (gameBoardIsNull) {
            log.error("GameBoard is null in received GameState! Stack trace:", new Exception("Stack trace"));
        }

        this.gameManager = state.gameManager();
        this.gameBoard = state.gameBoard();

        if (gameManager != null && gameBoard != null) {
            gameManager.relink(gameBoard);
        }

        String myPlayerName = ApplicationConfiguration.getInstance().getMyPlayerName();
        assert state.gameManager() != null;
        String currentPlayerName = state.gameManager().getCurrentPlayer().getName();

        if (currentPlayerName.equals(myPlayerName)) {
            setGameControlsEnabled(true);
            log.info("It's my turn!");
        } else {
            setGameControlsEnabled(false);
            log.info("Waiting for {}'s turn...", currentPlayerName);
        }

        updateAllUI();
    }

    public void onLocalPlayerMove(GameMove move) {
        PlayerType playerType = ApplicationConfiguration.getInstance().getPlayerType();

        if (playerType == PlayerType.HOST) {
            GameServerThread server = ApplicationConfiguration.getInstance().getGameServer();
            if (server != null) {
                server.broadcastGameState(new GameState(gameManager, gameBoard));
            }
        } else if (playerType == PlayerType.CLIENT) {
            GameClientThread client = ApplicationConfiguration.getInstance().getGameClient();
            if (client != null) {
                client.sendMove(move);
            }
        }
    }

    private void setupChat() {
        try {
            String hostname = ConfigurationReader.getStringValue(ConfigurationKey.HOSTNAME);
            int rmiPort = ConfigurationReader.getIntegerValue(ConfigurationKey.RMI_PORT);

            Registry registry = LocateRegistry.getRegistry(hostname, rmiPort);
            chatService = (ChatService) registry.lookup(ChatService.REMOTE_OBJECT_NAME);

            log.info("Connected to chat service");

            startChatPolling();

        } catch (Exception e) {
            log.error("Failed to connect to chat", e);
        }
    }

    @FXML
    private void sendChatMessage() {
        try {
            String message = chatInput.getText();
            if (!message.isEmpty()) {
                String playerName = gameManager.getCurrentPlayer().getName();
                chatService.sendChatMessage(playerName + ": " + message);
                chatInput.clear();
            }
        } catch (RemoteException e) {
            log.error("Failed to send chat message", e);
        }
    }

    private void startChatPolling() {
        Timeline chatPoll = new Timeline(new KeyFrame(Duration.seconds(1), _ -> {
            try {
                List<String> messages = chatService.returnChatHistory();
                Platform.runLater(() -> {
                    chatListView.getItems().clear();
                    chatListView.getItems().addAll(messages);
                });
            } catch (RemoteException e) {
                log.error("Chat polling error", e);
            }
        }));
        chatPoll.setCycleCount(Animation.INDEFINITE);
        chatPoll.play();
    }

    public void setupGame(GameState gameState) {
        log.info("Setting up game - gameBoard = {}", gameState.gameBoard() != null ? "NOT NULL" : "NULL");

        PlayerType playerType = ApplicationConfiguration.getInstance().getPlayerType();

        this.gameManager = gameState.gameManager();
        this.gameBoard = gameState.gameBoard();

        if (gameBoard == null) {
            log.error("GameBoard is null - cannot initialize!");
            return;
        }

        initializeComponents();

        if (playerType != PlayerType.LOCAL) {
            setupChat();
        }

        if (playerType == PlayerType.CLIENT) {
            GameClientThread client = ApplicationConfiguration.getInstance().getGameClient();
            if (client != null) {
                client.addGameStateListener(this::updateFromNetwork);
                log.info("CLIENT registered UI update listener");
            }
        }

        if (playerType == PlayerType.HOST) {
            GameServerThread server = ApplicationConfiguration.getInstance().getGameServer();
            if (server != null) {
                server.setLocalHostListener(this::updateFromNetwork);
            }
        }

        String myPlayerName = ApplicationConfiguration.getInstance().getMyPlayerName();
        if (myPlayerName != null) {
            Player myPlayer = gameManager.getPlayerByName(myPlayerName);
            if (myPlayer != null) {
                showPlayerBoard(myPlayer);
            }
        } else {
            showPlayerBoard(gameManager.getCurrentPlayer());
        }


        if (playerType == PlayerType.LOCAL || playerType == PlayerType.HOST) {
            this.gameManager.startGame();
        }

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
        this.placementManager = new PlacementManager(this, this.gameManager, this.gameBoard, this.actionManager);
        this.placementCoordinator = new PlacementCoordinator(this.placementManager);

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

        boolean isPlacing = (placementManager != null && placementManager.isPlacementMode());
        uiManager.updateGeneralUI(viewedPlayer, isPlacing);

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

        if (cancelPlacementButton != null) {
            cancelPlacementButton.setVisible(!isEnabled);
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