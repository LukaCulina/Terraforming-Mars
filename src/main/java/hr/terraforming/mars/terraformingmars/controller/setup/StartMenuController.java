package hr.terraforming.mars.terraformingmars.controller.setup;

import hr.terraforming.mars.terraformingmars.controller.game.GameScreenController;
import hr.terraforming.mars.terraformingmars.model.GameState;
import hr.terraforming.mars.terraformingmars.service.GameStateService;
import hr.terraforming.mars.terraformingmars.util.DialogUtils;
import hr.terraforming.mars.terraformingmars.util.ScreenUtils;
import hr.terraforming.mars.terraformingmars.view.ScreenNavigator;
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
        ScreenNavigator.showChooseModeScreen();
    }

    @FXML
    private void loadSavedGame() {
        log.info("Loading saved game...");
        GameState loadedState = gameStateService.loadGame();

        if (loadedState != null) {
            var result = ScreenUtils.loadFxml("GameScreen.fxml");
            GameScreenController controller = (GameScreenController) result.controller();
            Scene mainGameScene = ScreenUtils.createScene(result.root());

            controller.getSetupCoordinator().setupLoadedGame(loadedState);

            ScreenNavigator.getMainStage().setScene(mainGameScene);
            ScreenNavigator.getMainStage().setTitle("Terraforming Mars - Loaded Game");

            Platform.runLater(() ->
                    DialogUtils.showDialog("Game loaded successfully!")
            );
        } else {
            DialogUtils.showDialog("Failed to load game. No saved game found.");
        }
    }
}
