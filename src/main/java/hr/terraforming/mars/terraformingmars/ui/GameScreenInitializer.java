package hr.terraforming.mars.terraformingmars.ui;

import hr.terraforming.mars.terraformingmars.controller.game.GameScreenController;
import hr.terraforming.mars.terraformingmars.manager.ActionManager;
import hr.terraforming.mars.terraformingmars.manager.GameScreenManager;
import hr.terraforming.mars.terraformingmars.view.HexBoardDrawer;
import hr.terraforming.mars.terraformingmars.view.component.ActionPanelComponents;
import hr.terraforming.mars.terraformingmars.view.component.GlobalStatusComponents;
import hr.terraforming.mars.terraformingmars.view.component.PlayerControlComponents;

public class GameScreenInitializer {

    private GameScreenInitializer() {
        throw new IllegalStateException("Utility class");
    }

    public static GameScreenManager initUI(GameScreenController controller,
                                           ActionManager actionManager) {

        HexBoardDrawer hexBoardDrawer = new HexBoardDrawer(
                controller.getHexBoardPane(),
                controller.getGameBoard(),
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

        GameScreenManager ui = new GameScreenManager(
                controller, actionManager, hexBoardDrawer,
                statusComp, actionPanel, controls
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
