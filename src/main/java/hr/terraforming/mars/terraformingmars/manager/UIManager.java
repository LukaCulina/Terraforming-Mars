package hr.terraforming.mars.terraformingmars.manager;

import hr.terraforming.mars.terraformingmars.controller.TerraformingMarsController;
import hr.terraforming.mars.terraformingmars.enums.GamePhase;
import hr.terraforming.mars.terraformingmars.enums.Milestone;
import hr.terraforming.mars.terraformingmars.enums.ResourceType;
import hr.terraforming.mars.terraformingmars.enums.StandardProject;
import hr.terraforming.mars.terraformingmars.model.GameBoard;
import hr.terraforming.mars.terraformingmars.model.GameManager;
import hr.terraforming.mars.terraformingmars.model.Player;
import hr.terraforming.mars.terraformingmars.view.UIComponentBuilder;
import hr.terraforming.mars.terraformingmars.view.HexBoardDrawer;
import hr.terraforming.mars.terraformingmars.view.components.ActionPanelComponents;
import hr.terraforming.mars.terraformingmars.view.components.GlobalStatusComponents;
import hr.terraforming.mars.terraformingmars.view.components.PlayerControlComponents;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.*;
import lombok.Getter;

import java.util.Map;

public class UIManager {

    @Getter
    private final HexBoardDrawer hexBoardDrawer;

    private GameBoard gameBoard;
    private GameManager gameManager;
    private final ActionManager actionManager;
    private final GlobalStatusComponents globalStatus;
    private final ActionPanelComponents actionPanels;
    private final PlayerControlComponents playerControls;

    public UIManager(GameBoard gameBoard, GameManager gameManager, ActionManager actionManager,
                     HexBoardDrawer hexBoardDrawer, GlobalStatusComponents globalStatus,
                     ActionPanelComponents actionPanels, PlayerControlComponents playerControls) {
        this.gameBoard = gameBoard;
        this.gameManager = gameManager;
        this.actionManager = actionManager;
        this.hexBoardDrawer = hexBoardDrawer;
        this.globalStatus = globalStatus;
        this.actionPanels = actionPanels;
        this.playerControls = playerControls;
    }

    public void updateGameState(GameManager newManager, GameBoard newBoard) {
        this.gameManager = newManager;
        this.gameBoard = newBoard;
        if (this.hexBoardDrawer != null) {
            this.hexBoardDrawer.setGameBoard(newBoard);
        }
    }

    public void initializeUIComponents(TerraformingMarsController controller, BorderPane gameBoardPane, VBox playerInterface, GridPane bottomGrid, StackPane temperaturePane) {
        UIComponentBuilder componentBuilder = new UIComponentBuilder(controller, actionManager, gameManager);
        componentBuilder.createPlayerButtons(playerControls.playerListBar());
        componentBuilder.createMilestoneButtons(actionPanels.milestonesBox());
        componentBuilder.createStandardProjectButtons(actionPanels.standardProjectsBox());

        playerControls.passTurnButton().setOnAction(_ -> actionManager.handlePassTurn());
        playerControls.convertHeatButton().setOnAction(_ -> actionManager.handleConvertHeat());
        playerControls.convertPlantsButton().setOnAction(_ -> actionManager.handleConvertPlants());

        playerControls.passTurnButton().prefWidthProperty().bind(playerInterface.widthProperty().multiply(0.6));

        VBox conversionBox = (VBox) playerControls.convertHeatButton().getParent();
        playerControls.convertHeatButton().prefWidthProperty().bind(conversionBox.widthProperty().multiply(0.7));
        playerControls.convertPlantsButton().prefWidthProperty().bind(conversionBox.widthProperty().multiply(0.7));

        actionPanels.standardProjectsBox().prefWidthProperty().bind(gameBoardPane.widthProperty().multiply(0.15));
        globalStatus.oxygenProgressBar().prefWidthProperty().bind(gameBoardPane.widthProperty().multiply(0.8));
        temperaturePane.prefWidthProperty().bind(gameBoardPane.widthProperty().multiply(0.15));
        globalStatus.temperatureProgressBar().prefWidthProperty().bind(gameBoardPane.widthProperty().multiply(0.6));
        globalStatus.oceansLabel().prefWidthProperty().bind(bottomGrid.widthProperty().multiply(0.15));
        globalStatus.oceansLabel().prefHeightProperty().bind(globalStatus.oceansLabel().prefWidthProperty());
        actionPanels.milestonesBox().prefWidthProperty().bind(bottomGrid.widthProperty().multiply(0.40));
    }

    public void updateGeneralUI(Player viewedPlayer, boolean isPlacing) {
        updateGlobalParameters();
        updateStandardProjectButtonsState(isPlacing);
        updateMilestoneButtonsState(isPlacing);
        updatePlayerButtonsHighlight(viewedPlayer);
        updateConvertButtonsState(isPlacing);
        drawBoard();
    }

    private void drawBoard() {
        if (hexBoardDrawer != null) {
            hexBoardDrawer.drawBoard();
        }
    }

    private void updateGlobalParameters() {
        globalStatus.generationLabel().setText("Generation: " + gameManager.getGeneration());
        globalStatus.phaseLabel().setText("Phase: " + gameManager.getCurrentPhase().toString());

        double oxygenProgress = (double) gameBoard.getOxygenLevel() / GameBoard.MAX_OXYGEN;
        globalStatus.oxygenProgressBar().setProgress(oxygenProgress);
        globalStatus.oxygenLabel().setText(String.format("%d%%", gameBoard.getOxygenLevel()));

        double tempProgress = (double) (gameBoard.getTemperature() - GameBoard.MIN_TEMPERATURE) / (GameBoard.MAX_TEMPERATURE - GameBoard.MIN_TEMPERATURE);
        globalStatus.temperatureProgressBar().setProgress(tempProgress);
        globalStatus.temperatureLabel().setText(String.format("%d°C", gameBoard.getTemperature()));

        if (globalStatus.oceansLabel() != null) {
            int oceans = gameBoard.getOceansPlaced();
            int maxOceans = GameBoard.MAX_OCEANS;
            globalStatus.oceansLabel().setText(String.format("%d / %d", oceans, maxOceans));
        }
    }

    private void updateStandardProjectButtonsState(boolean isPlacing) {
        Player currentPlayer = gameManager.getCurrentPlayer();
        boolean isActionPhase = gameManager.getCurrentPhase() == GamePhase.ACTIONS;
        boolean canPerformAction = gameManager.getActionsTakenThisTurn() < 2;

        actionPanels.standardProjectsBox().getChildren().forEach(node -> {
            if (node instanceof Button btn) {
                StandardProject project = (StandardProject) btn.getUserData();
                if (project != null) {
                    boolean canAfford = currentPlayer.getMC() >= project.getCost();
                    boolean disable = isPlacing || !canAfford || !isActionPhase || !canPerformAction;
                    if (project == StandardProject.AQUIFER) {
                        disable = disable || !gameBoard.canPlaceOcean();
                    }
                    btn.setDisable(disable);
                }
            }
        });
        //playerControls.passTurnButton().setDisable(isPlacing || !isActionPhase);
    }

    private void updateMilestoneButtonsState(boolean isPlacing) {
        Player currentPlayer = gameManager.getCurrentPlayer();
        boolean isActionPhase = gameManager.getCurrentPhase() == GamePhase.ACTIONS;
        Map<Milestone, Player> claimed = gameBoard.getClaimedMilestones();

        actionPanels.milestonesBox().getChildren().forEach(node -> {
            if (node instanceof Button btn) {
                Milestone milestone = (Milestone) btn.getUserData();
                if (milestone != null) {
                    if (claimed.containsKey(milestone)) {
                        Player owner = claimed.get(milestone);
                        btn.setText(milestone.getName() + " (" + owner.getName() + ")");
                        btn.setDisable(true);
                        btn.setStyle("-fx-background-color: #555;");
                    } else {
                        boolean canAfford = currentPlayer.getMC() >= 8;
                        boolean requirementMet = milestone.canClaim(currentPlayer);
                        btn.setDisable(isPlacing || !isActionPhase || !canAfford || !requirementMet || claimed.size() >= GameBoard.MAX_MILESTONES);
                        btn.setText(milestone.getName());
                        btn.setStyle("");
                    }
                }
            }
        });
    }

    private void updatePlayerButtonsHighlight(Player viewedPlayer) {
        Player currentPlayer = gameManager.getCurrentPlayer();
        for (Node node : playerControls.playerListBar().getChildren()) {
            if (node instanceof Button btn) {
                String buttonPlayerName = btn.getText();
                btn.getStyleClass().removeAll("player-button-active", "player-button-viewed");

                if (currentPlayer.getName().equals(buttonPlayerName)) {
                    btn.getStyleClass().add("player-button-active");
                }
                if (viewedPlayer != null && viewedPlayer.getName().equals(buttonPlayerName)) {
                    btn.getStyleClass().add("player-button-viewed");
                }
            }
        }
    }

    private void updateConvertButtonsState(boolean isPlacing) {
        Player currentPlayer = gameManager.getCurrentPlayer();
        boolean isActionPhase = gameManager.getCurrentPhase() == GamePhase.ACTIONS;
        boolean canPerformAction = gameManager.getActionsTakenThisTurn() < 2;

        Button convertHeatBtn = playerControls.convertHeatButton();
        Button convertPlantsBtn = playerControls.convertPlantsButton();

        if (convertHeatBtn != null) {
            boolean canAffordHeat = currentPlayer.resourceProperty(ResourceType.HEAT).get() >= 8;
            boolean isTemperatureMaxed = gameBoard.getTemperature() >= GameBoard.MAX_TEMPERATURE;
            convertHeatBtn.setDisable(isPlacing || !isActionPhase || !canPerformAction || !canAffordHeat || isTemperatureMaxed);
        }

        if (convertPlantsBtn != null) {
            boolean canAffordPlants = currentPlayer.resourceProperty(ResourceType.PLANTS).get() >= currentPlayer.getGreeneryCost();
            convertPlantsBtn.setDisable(isPlacing || !isActionPhase || !canPerformAction || !canAffordPlants);
        }
    }
}
