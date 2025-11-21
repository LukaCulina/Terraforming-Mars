package hr.terraforming.mars.terraformingmars.controller;

import hr.terraforming.mars.terraformingmars.model.GameState;
import hr.terraforming.mars.terraformingmars.service.GameStateService;
import hr.terraforming.mars.terraformingmars.util.DialogUtils;
import hr.terraforming.mars.terraformingmars.util.ScreenLoader;
import hr.terraforming.mars.terraformingmars.view.GameScreens;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StartMenuController {
    private final GameStateService gameStateService = new GameStateService();

    @FXML
    private void startNewGame() {
        log.info("Starting new game...");
        gameStateService.clearGameData();
        GameScreens.showChooseModeScreen();
    }

    @FXML
    private void loadSavedGame() {
        log.info("Loading saved game...");
        GameState loadedState = gameStateService.loadGame();

        if (loadedState != null) {
            var result = ScreenLoader.loadFxml("GameScreen.fxml");
            TerraformingMarsController controller = (TerraformingMarsController) result.controller();
            Scene mainGameScene = ScreenLoader.createScene(result.root());

            controller.gameManager = loadedState.gameManager();
            controller.setGameBoard(loadedState.gameBoard());
            controller.getGameManager().relink(controller.getGameBoard());
            controller.initializeComponents();
            controller.setViewedPlayer(controller.getGameManager().getCurrentPlayer());
            controller.updateAllUI();

            GameScreens.getMainStage().setScene(mainGameScene);
            GameScreens.getMainStage().setTitle("Terraforming Mars - Loaded Game");

            Platform.runLater(() ->
                    DialogUtils.showDialog("Game loaded successfully!")
            );
        } else {
            DialogUtils.showDialog("Failed to load game. No saved game found.");
        }
    }
}
