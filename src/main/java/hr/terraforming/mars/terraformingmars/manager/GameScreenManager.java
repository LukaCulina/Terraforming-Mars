package hr.terraforming.mars.terraformingmars.manager;

import hr.terraforming.mars.terraformingmars.controller.game.GameScreenController;
import hr.terraforming.mars.terraformingmars.enums.GamePhase;
import hr.terraforming.mars.terraformingmars.enums.Milestone;
import hr.terraforming.mars.terraformingmars.enums.ResourceType;
import hr.terraforming.mars.terraformingmars.enums.StandardProject;
import hr.terraforming.mars.terraformingmars.model.GameBoard;
import hr.terraforming.mars.terraformingmars.model.GameManager;
import hr.terraforming.mars.terraformingmars.model.Player;
import hr.terraforming.mars.terraformingmars.view.HexBoardDrawer;
import hr.terraforming.mars.terraformingmars.view.component.ActionPanelComponents;
import hr.terraforming.mars.terraformingmars.view.component.GlobalStatusComponents;
import hr.terraforming.mars.terraformingmars.view.component.PlayerControlComponents;
import javafx.scene.Node;
import javafx.scene.control.Button;
import lombok.Getter;

import java.util.Map;

public class GameScreenManager {

    @Getter private final HexBoardDrawer hexBoardDrawer;
    private final GameScreenController controller;
    private final GlobalStatusComponents globalStatus;
    private final ActionPanelComponents actionPanels;
    private final PlayerControlComponents playerControls;

    public GameScreenManager(GameScreenController controller,
                             HexBoardDrawer hexBoardDrawer, GlobalStatusComponents globalStatus,
                             ActionPanelComponents actionPanels, PlayerControlComponents playerControls) {
        this.controller = controller;
        this.hexBoardDrawer = hexBoardDrawer;
        this.globalStatus = globalStatus;
        this.actionPanels = actionPanels;
        this.playerControls = playerControls;
    }

    private GameManager gm() { return controller.getGameManager(); }
    private GameBoard board() { return controller.getGameBoard(); }

    public void updateHexBoardDrawer() {
        if (hexBoardDrawer != null) {
            hexBoardDrawer.setGameBoard(board());
        }
    }

    public void updateGeneralUI(Player viewedPlayer, boolean isPlacing, boolean isMyTurn) {
        updateGlobalParameters();
        updateStandardProjectButtonsState(isPlacing);
        updateMilestoneButtonsState(isPlacing);
        updatePlayerButtonsHighlight(viewedPlayer);
        updateConvertButtonsState(isPlacing, isMyTurn);
        drawBoard();
    }

    private void drawBoard() {
        if (hexBoardDrawer != null) {
            hexBoardDrawer.drawBoard();
        }
    }

    private void updateGlobalParameters() {
        globalStatus.generationLabel().setText("Generation: " + gm().getGeneration());
        globalStatus.phaseLabel().setText("Phase: " + gm().getCurrentPhase().toString());

        double oxygenProgress = (double) board().getOxygenLevel() / GameBoard.MAX_OXYGEN;
        globalStatus.oxygenProgressBar().setProgress(oxygenProgress);
        globalStatus.oxygenLabel().setText(String.format("%d%%", board().getOxygenLevel()));

        double tempProgress = (double) (board().getTemperature() - GameBoard.MIN_TEMPERATURE) / (GameBoard.MAX_TEMPERATURE - GameBoard.MIN_TEMPERATURE);
        globalStatus.temperatureProgressBar().setProgress(tempProgress);
        globalStatus.temperatureLabel().setText(String.format("%d°C", board().getTemperature()));

        if (globalStatus.oceansLabel() != null) {
            int oceans = board().getOceansPlaced();
            int maxOceans = GameBoard.MAX_OCEANS;
            globalStatus.oceansLabel().setText(String.format("%d / %d", oceans, maxOceans));
        }
    }

    private void updateStandardProjectButtonsState(boolean isPlacing) {
        Player currentPlayer = gm().getCurrentPlayer();
        boolean isActionPhase = gm().getCurrentPhase() == GamePhase.ACTIONS;
        boolean canPerformAction = gm().getActionsTakenThisTurn() < 2;

        actionPanels.standardProjectsBox().getChildren().forEach(node -> {
            if (node instanceof Button btn) {
                StandardProject project = (StandardProject) btn.getUserData();
                if (project != null) {
                    boolean canAfford = currentPlayer.getMC() >= project.getCost();
                    boolean disable = isPlacing || !canAfford || !isActionPhase || !canPerformAction;
                    if (project == StandardProject.AQUIFER) {
                        disable = disable || !board().canPlaceOcean();
                    } else if (project == StandardProject.SELL_PATENTS) {
                        boolean hasCardsInHand = !currentPlayer.getHand().isEmpty();
                        disable = disable || !hasCardsInHand;
                    }
                    btn.setDisable(disable);
                }
            }
        });
    }

    private void updateMilestoneButtonsState(boolean isPlacing) {
        Player currentPlayer = gm().getCurrentPlayer();
        boolean isActionPhase = gm().getCurrentPhase() == GamePhase.ACTIONS;
        Map<Milestone, Player> claimed = board().getClaimedMilestones();

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
        Player currentPlayer = gm().getCurrentPlayer();
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

    private void updateConvertButtonsState(boolean isPlacing, boolean isMyTurn) {
        Player currentPlayer = gm().getCurrentPlayer();
        boolean isActionPhase = gm().getCurrentPhase() == GamePhase.ACTIONS;
        boolean canPerformAction = gm().getActionsTakenThisTurn() < 2;
        boolean controlsActive = isMyTurn && isActionPhase && canPerformAction && !isPlacing;

        Button convertHeatBtn = playerControls.convertHeatButton();
        Button convertPlantsBtn = playerControls.convertPlantsButton();

        if (convertHeatBtn != null) {
            boolean canAffordHeat = currentPlayer.resourceProperty(ResourceType.HEAT).get() >= 8;
            boolean isTemperatureMaxed = board().getTemperature() >= GameBoard.MAX_TEMPERATURE;
            convertHeatBtn.setDisable(!controlsActive || !canAffordHeat || isTemperatureMaxed);
        }

        if (convertPlantsBtn != null) {
            boolean canAffordPlants = currentPlayer.resourceProperty(ResourceType.PLANTS).get() >= currentPlayer.getGreeneryCost();
            convertPlantsBtn.setDisable(!controlsActive || !canAffordPlants);
        }
    }
}