package hr.terraforming.mars.terraformingmars.controller;

import hr.terraforming.mars.terraformingmars.model.GameState;
import hr.terraforming.mars.terraformingmars.service.SaveLoadService;
import hr.terraforming.mars.terraformingmars.util.DialogUtils;
import hr.terraforming.mars.terraformingmars.view.GameScreens;
import javafx.application.Platform;
import javafx.fxml.FXML;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StartMenuController {
    private final SaveLoadService saveLoadService = new SaveLoadService();
    private final TerraformingMarsController terraformingMarsController= new TerraformingMarsController();

    @FXML
    private void onStartNewGame() {
        log.info("Starting new game...");
        //GameScreens.showChoosePlayersScreen();
        terraformingMarsController.startNewGame();
    }

    @FXML
    private void onLoadGame() {
        log.info("Loading saved game...");
        GameState loadedState = saveLoadService.loadGame();

        if (loadedState != null) {
            GameScreens.showGameScreen(loadedState);
            Platform.runLater(() ->
                    DialogUtils.showDialog("Game loaded successfully!")
            );
        } else {
            DialogUtils.showDialog("Failed to load game. No saved game found.");
        }
    }
}
