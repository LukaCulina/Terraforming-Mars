package hr.terraforming.mars.terraformingmars.ui;

import hr.terraforming.mars.terraformingmars.controller.game.GameScreenController;
import hr.terraforming.mars.terraformingmars.manager.ActionManager;
import hr.terraforming.mars.terraformingmars.manager.GameScreenManager;
import hr.terraforming.mars.terraformingmars.view.HexBoardDrawer;
import hr.terraforming.mars.terraformingmars.view.UIComponentBuilder;
import hr.terraforming.mars.terraformingmars.view.component.ActionPanelComponents;
import hr.terraforming.mars.terraformingmars.view.component.GlobalStatusComponents;
import hr.terraforming.mars.terraformingmars.view.component.PlayerControlComponents;
import javafx.scene.layout.*;

public class GameScreenInitializer {

    private GameScreenInitializer() {
        throw new IllegalStateException("Utility class");
    }

    public static GameScreenManager initializeUI(GameScreenController controller,
                                                 ActionManager actionManager) {

        HexBoardDrawer hexBoardDrawer = new HexBoardDrawer(
                controller.getHexBoardPane(),
                controller.getGameBoard(),
                controller.getPlacementManager()
        );

        GlobalStatusComponents statusComponents = new GlobalStatusComponents(
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

        GameScreenManager gameScreen = new GameScreenManager(
                controller, hexBoardDrawer,
                statusComponents, actionPanel, controls
        );

        initializeUIComponents(controller, actionManager, actionPanel, controls);
        setupBindings(controller, statusComponents, actionPanel, controls);
        setupResponsiveFonts(controller.gameBoardPane);

        return gameScreen;
    }

    private static void initializeUIComponents(GameScreenController controller,
                                               ActionManager actionManager,
                                               ActionPanelComponents actionPanels,
                                               PlayerControlComponents playerControls) {
        UIComponentBuilder componentBuilder = new UIComponentBuilder(
                controller, actionManager, controller.getGameManager()
        );
        componentBuilder.createPlayerButtons(playerControls.playerListBar());
        componentBuilder.createMilestoneButtons(actionPanels.milestonesBox());
        componentBuilder.createStandardProjectButtons(actionPanels.standardProjectsBox());

        playerControls.passTurnButton().setOnAction(_ -> actionManager.handlePassTurn());
        playerControls.convertHeatButton().setOnAction(_ -> actionManager.handleConvertHeat());
        playerControls.convertPlantsButton().setOnAction(_ -> actionManager.handleConvertPlants());
    }

    private static void setupBindings(GameScreenController controller,
                                      GlobalStatusComponents globalStatus,
                                      ActionPanelComponents actionPanels,
                                      PlayerControlComponents playerControls) {
        BorderPane gameBoardPane = controller.gameBoardPane;
        BorderPane playerInterface = controller.playerInterface;
        GridPane bottomGrid = controller.bottomGrid;
        StackPane temperaturePane = controller.temperaturePane;

        playerControls.passTurnButton().prefWidthProperty()
                .bind(playerInterface.widthProperty().multiply(0.6));

        VBox conversionBox = (VBox) playerControls.convertHeatButton().getParent();
        playerControls.convertHeatButton().prefWidthProperty()
                .bind(conversionBox.widthProperty().multiply(0.8));
        playerControls.convertPlantsButton().prefWidthProperty()
                .bind(conversionBox.widthProperty().multiply(0.8));

        actionPanels.standardProjectsBox().prefWidthProperty()
                .bind(gameBoardPane.widthProperty().multiply(0.15));
        actionPanels.milestonesBox().prefWidthProperty()
                .bind(bottomGrid.widthProperty().multiply(0.4));

        globalStatus.oxygenProgressBar().prefWidthProperty()
                .bind(gameBoardPane.widthProperty().multiply(0.8));
        temperaturePane.prefWidthProperty()
                .bind(gameBoardPane.widthProperty().multiply(0.15));
        globalStatus.temperatureProgressBar().prefWidthProperty()
                .bind(gameBoardPane.widthProperty().multiply(0.6));
        globalStatus.oceansLabel().prefWidthProperty()
                .bind(bottomGrid.widthProperty().multiply(0.15));
        globalStatus.oceansLabel().prefHeightProperty()
                .bind(globalStatus.oceansLabel().prefWidthProperty());
    }

    private static void setupResponsiveFonts(BorderPane gameBoardPane) {
        GameScreenResizer.attachFontResizeListeners(gameBoardPane, () ->
                GameScreenResizer.updateFonts(gameBoardPane,
                        new GameScreenResizer.FontMapping(".convert-button", 0.015),
                        new GameScreenResizer.FontMapping(".project-milestone", 0.025)
                )
        );
    }
}
