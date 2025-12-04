package hr.terraforming.mars.terraformingmars.controller;

import hr.terraforming.mars.terraformingmars.enums.PlayerType;
import hr.terraforming.mars.terraformingmars.model.ApplicationConfiguration;
import hr.terraforming.mars.terraformingmars.view.ScreenNavigator;
import javafx.fxml.FXML;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ChooseOnlineModeController {
    @FXML
    private void hostGame() {
        ApplicationConfiguration.getInstance().setPlayerType(PlayerType.HOST);

        ScreenNavigator.showChoosePlayersScreen();
    }

    @FXML
    private void joinGame() {
        ApplicationConfiguration.getInstance().setPlayerType(PlayerType.CLIENT);

        ScreenNavigator.showJoinGameScreen();
    }

    @FXML
    private void returnToPreviousScreen() {
        ScreenNavigator.showChooseModeScreen();
    }
}