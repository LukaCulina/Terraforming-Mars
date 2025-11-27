package hr.terraforming.mars.terraformingmars.controller;

import hr.terraforming.mars.terraformingmars.chat.ChatService;
import hr.terraforming.mars.terraformingmars.enums.ActionType;
import hr.terraforming.mars.terraformingmars.enums.GamePhase;
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
import java.time.LocalDateTime;
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

    public void updateFromNetwork(GameState state) {

        if (state == null) {
            log.error("Received null GameState!");
            return;
        }

        if (state.gameManager() == null || state.gameBoard() == null) {
            log.error("Critical error: Received incomplete GameState! Manager={}, Board={}",
                    state.gameManager(), state.gameBoard());
            return;
        }

        log.debug("🔄 NetUpdate: Gen={}, Phase={}, CurrentTurn={}, MyName={}",
                state.gameManager().getGeneration(),
                state.gameManager().getCurrentPhase(),
                state.gameManager().getCurrentPlayer().getName(),
                ApplicationConfiguration.getInstance().getMyPlayerName());

        this.gameManager = state.gameManager();
        this.gameBoard = state.gameBoard();

        gameManager.relink(gameBoard);

        if (uiManager != null) {
            uiManager.updateGameState(this.gameManager, this.gameBoard);
        }

        if (actionManager != null) {
            actionManager.updateState(this.gameManager, this.gameBoard);
        }

        String myPlayerName = ApplicationConfiguration.getInstance().getMyPlayerName();
        String currentPlayerName = state.gameManager().getCurrentPlayer().getName();
        log.info("🔄 NETWORK UPDATE: My Name='{}', Current Turn='{}'", myPlayerName, currentPlayerName);

        if (myPlayerName != null) {
            Player myPlayer = gameManager.getPlayerByName(myPlayerName);
            if (myPlayer != null) {
                this.viewedPlayer = myPlayer;
            }
        } else {
            this.viewedPlayer = gameManager.getCurrentPlayer();
        }

        PlayerType playerType = ApplicationConfiguration.getInstance().getPlayerType();

        boolean isMyTurn = currentPlayerName.equals(myPlayerName);
        boolean isActionPhase = gameManager.getCurrentPhase() == hr.terraforming.mars.terraformingmars.enums.GamePhase.ACTIONS;
        log.info("👉 Is it my turn? {}", isMyTurn);

        if (isMyTurn && isActionPhase) {
            setGameControlsEnabled(true);
            log.info("CONTROLS ENABLED for {}", myPlayerName);
            Platform.runLater(this::updateAllUI);
        } else {
            setGameControlsEnabled(false);
            log.info("CONTROLS DISABLED (MyTurn: {}, ActionPhase: {})", isMyTurn, isActionPhase);
        }
        if (gameManager.getCurrentPhase() == GamePhase.RESEARCH) {
            Player currentResearchPlayer = gameManager.getCurrentPlayerForDraft();

            log.debug("🔍 Research Check: current={}, me={}",
                    (currentResearchPlayer != null ? currentResearchPlayer.getName() : "NULL"),
                    myPlayerName);

            if (playerType == PlayerType.CLIENT && currentResearchPlayer != null) {
                assert myPlayerName != null;
                if (myPlayerName.equals(currentResearchPlayer.getName())) {
                    log.info("Opening research modal for player '{}'", currentResearchPlayer.getName());

                    if (!isResearchModalOpen) {
                        isResearchModalOpen = true;
                        Platform.runLater(() -> openResearchModalForClient(currentResearchPlayer));
                    }
                }
            }
        } else {
            isResearchModalOpen = false;
        }

        updateAllUI();
        updatePlayerHighlight(gameManager.getCurrentPlayer());
    }

    private boolean isResearchModalOpen = false;

    private void openResearchModalForClient(Player player) {
        List<Card> offer = gameManager.drawCards(4);

        log.info("🎴 CLIENT: Opening research modal for {}", player.getName());

        ScreenLoader.showAsModal(
                getSceneWindow(),
                "ChooseCards.fxml",
                "Research Phase - " + player.getName(),
                0.7, 0.8,
                (ChooseCardsController c) ->
                        c.setup(
                        player,
                        offer,
                        boughtCards -> finishResearchForClient(player, boughtCards),
                        gameManager,
                        true
                )
        );
    }

    private void finishResearchForClient(Player player, List<Card> boughtCards) {
        int cost = boughtCards.size() * 3;
        if (player.spendMC(cost)) {
            player.getHand().addAll(boughtCards);
        }

        if (!boughtCards.isEmpty()) {
            String details = boughtCards.stream().map(Card::getName).reduce((a,b) -> a + "," + b).orElse("");
            GameMove move = new GameMove(
                    player.getName(),
                    ActionType.OPEN_CHOOSE_CARDS_MODAL,
                    details,
                    LocalDateTime.now()
            );
            actionManager.recordAndSaveMove(move);
        }

        isResearchModalOpen = false;
        log.info("✅ CLIENT: Research complete for {}", player.getName());
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

        if (gameManager != null) {
            gameManager.relink(gameBoard);
        }

        initializeComponents();

        if (playerType != PlayerType.LOCAL) {
            setupChat();
        }

        if (playerType == PlayerType.CLIENT) {
            setGameControlsEnabled(false);
            log.info("🚫 CLIENT controls disabled on setup (waiting for host turn info)");
            GameClientThread client = ApplicationConfiguration.getInstance().getGameClient();
            if (client != null) {
                client.addGameStateListener(this::updateFromNetwork);
                log.info("CLIENT registered UI update listener");
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