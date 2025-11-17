    package hr.terraforming.mars.terraformingmars.controller;

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
    import javafx.scene.control.*;
    import javafx.scene.layout.*;
    import javafx.stage.Window;
    import lombok.Getter;
    import lombok.Setter;
    import lombok.extern.slf4j.Slf4j;

    import java.time.format.DateTimeFormatter;
    import java.util.List;

    @Slf4j
    public class TerraformingMarsController {

        @Getter
        @FXML private AnchorPane hexBoardPane;
        @FXML
        public BorderPane gameBoardPane;
        @FXML
        public StackPane temperaturePane;
        @FXML
        public GridPane bottomGrid;
        @FXML private VBox currentPlayerBoardContainer;
        @FXML
        public HBox playerListBar;
        @FXML
        public VBox standardProjectsBox;
        @FXML
        public ProgressBar oxygenProgressBar;
        @FXML
        public Label oxygenLabel;
        @FXML
        public ProgressBar temperatureProgressBar;
        @FXML
        public Label temperatureLabel;
        @FXML
        public Label generationLabel;
        @FXML
        public Label phaseLabel;
        @FXML
        public Button passTurnButton;
        @FXML
        public Button convertHeatButton;
        @FXML
        public Button convertPlantsButton;
        @FXML
        public Label oceansLabel;
        @FXML
        public VBox milestonesBox;
        @FXML private Button cancelPlacementButton;
        @FXML
        public VBox playerInterface;
        @FXML private Label lastMoveLabel;

        @Getter
        private PlacementManager placementManager;
        @Getter
        private UIManager uiManager;
        @Getter
        private ActionManager actionManager;
        @Getter
        GameManager gameManager;
        @Setter
        @Getter
        private GameBoard gameBoard;
        private PlayerBoardController currentPlayerBoardController;
        @Getter
        private PlacementCoordinator placementCoordinator;
        @Setter
        private Player viewedPlayer = null;
        private final GameStateService gameStateService = new GameStateService();
        private ReplayManager replayManager;
        private Timeline moveHistoryTimeline;

        @FXML
        private void initialize() {
            currentPlayerBoardController = PlayerBoardLoader.loadPlayerBoard(currentPlayerBoardContainer);
        }

        public void setupGame(GameState gameState) {
            this.gameManager = gameState.gameManager();
            this.gameBoard = gameState.gameBoard();
            initializeComponents();
            this.gameManager.startGame();
            this.viewedPlayer = this.gameManager.getCurrentPlayer();
            Platform.runLater(() -> gameBoardPane.maxHeightProperty().bind(
                    hexBoardPane.getScene().heightProperty().subtract(28)
            ));
            Platform.runLater(this::updateAllUI);
            startMoveHistory();
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
            GameScreens.showChoosePlayersScreen();
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