    package hr.terraforming.mars.terraformingmars.controller;

    import hr.terraforming.mars.terraformingmars.replay.ReplayManager;
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
    import lombok.Getter;
    import lombok.Setter;
    import lombok.extern.slf4j.Slf4j;

    import java.io.*;
    import java.time.format.DateTimeFormatter;
    import java.util.List;

    @Slf4j
    public class TerraformingMarsController {

        @Getter
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
        @Getter
        private UIManager uiManager;
        @Getter
        private ActionManager actionManager;
        @Getter
        private GameManager gameManager;
        @Setter
        @Getter
        private GameBoard gameBoard;
        private PlayerBoardController currentPlayerBoardController;
        @Getter
        private PlacementCoordinator placementCoordinator;
        @Setter
        private Player viewedPlayer = null;
        private final SaveLoadService saveLoadService = new SaveLoadService();
        private ReplayManager replayManager;
        private Timeline moveHistoryTimeline;

        @FXML
        private void initialize() {
            loadPlayerBoard();
        }

        private void loadPlayerBoard() {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/hr/terraforming/mars/terraformingmars/PlayerBoard.fxml"));
                Node boardNode = loader.load();
                currentPlayerBoardController = loader.getController();
                currentPlayerBoardContainer.getChildren().setAll(boardNode);
            } catch (IOException e) {
                log.error("FATAL: Could not load PlayerBoard.fxml. The application cannot start correctly.", e);
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

        public void initializeComponents() {
            GameFlowManager gameFlowManager = new GameFlowManager(this, this.gameManager, this.gameBoard);
            this.actionManager = new ActionManager(this, this.gameManager, this.gameBoard, gameFlowManager);
            this.placementManager = new PlacementManager(this, this.gameManager, this.gameBoard, this.actionManager);
            this.placementCoordinator = new PlacementCoordinator(this.placementManager);
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

        public void startMoveHistory() {
            if (moveHistoryTimeline == null) {
                moveHistoryTimeline = GameMoveUtils.createLastMoveTimeline(lastMoveLabel);
                moveHistoryTimeline.play();
            }
        }

        public void updateAllUI() {
            if (uiManager == null || gameManager == null || viewedPlayer == null) {
                log.warn("UI update skipped because a critical component is null.");
                return;
            }

            boolean isPlacing = (placementManager != null && placementManager.isPlacementMode());
            uiManager.updateGeneralUI(viewedPlayer, isPlacing);

            if (currentPlayerBoardController != null) {
                currentPlayerBoardController.setPlayer(viewedPlayer, actionManager);
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

        public void drawBoard() {
            updateAllUI();
        }

        public void setGameControlsEnabled(boolean isEnabled) {

            List<Node> nodesToToggle = List.of(
                    passTurnButton, convertHeatButton, convertPlantsButton,
                    standardProjectsBox, milestonesBox
            );

            nodesToToggle.forEach(node -> {
                if (node != null) {
                    node.setDisable(!isEnabled);
                }
            });

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