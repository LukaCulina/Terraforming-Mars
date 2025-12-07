package hr.terraforming.mars.terraformingmars.ui;

import hr.terraforming.mars.terraformingmars.controller.game.TerraformingMarsController;
import hr.terraforming.mars.terraformingmars.manager.ActionManager;
import hr.terraforming.mars.terraformingmars.manager.UIManager;
import hr.terraforming.mars.terraformingmars.model.GameBoard;
import hr.terraforming.mars.terraformingmars.model.GameManager;
import hr.terraforming.mars.terraformingmars.view.HexBoardDrawer;
import hr.terraforming.mars.terraformingmars.view.component.ActionPanelComponents;
import hr.terraforming.mars.terraformingmars.view.component.GlobalStatusComponents;
import hr.terraforming.mars.terraformingmars.view.component.PlayerControlComponents;

public class UIInitializer {

    private UIInitializer() {
        throw new IllegalStateException("Utility class");
    }

    public static UIManager initUI(TerraformingMarsController controller,
                                   GameBoard gameBoard,
                                   GameManager gameManager,
                                   ActionManager actionManager) {

        HexBoardDrawer hexBoardDrawer = new HexBoardDrawer(
                controller.getHexBoardPane(),
                gameBoard,
                controller.getPlacementManager()
        );

        GlobalStatusComponents statusComp = new GlobalStatusComponents(
                controller.oxygenProgressBar,
                controller.oxygenLabel,
                controller.temperatureProgressBar,
                controller.temperatureLabel,
                controller.oceansLabel,
                controller.generationLabel,
                controller.phaseLabel
        );

        ActionPanelComponents actionPanel = new ActionPanelComponents(
                controller.milestonesBox, controller.standardProjectsBox
        );

        PlayerControlComponents controls = new PlayerControlComponents(
                controller.playerListBar,
                controller.passTurnButton,
                controller.convertHeatButton,
                controller.convertPlantsButton
        );

        UIManager ui = new UIManager(
                gameBoard, gameManager, actionManager,
                hexBoardDrawer, statusComp, actionPanel, controls
        );

        ui.initializeUIComponents(
                controller,
                controller.gameBoardPane,
                controller.playerInterface,
                controller.bottomGrid,
                controller.temperaturePane
        );

        return ui;
    }
}
