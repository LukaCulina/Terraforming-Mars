package hr.terraforming.mars.terraformingmars.controller;

import hr.terraforming.mars.terraformingmars.enums.PlayerType;
import hr.terraforming.mars.terraformingmars.model.ApplicationConfiguration;
import hr.terraforming.mars.terraformingmars.view.GameScreens;
import javafx.fxml.FXML;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ChooseOnlineModeController {
    @FXML
    private void hostGame() {
        ApplicationConfiguration.getInstance().setPlayerType(PlayerType.HOST);

        GameScreens.showChoosePlayersScreen();
    }

    @FXML
    private void joinGame() {
        ApplicationConfiguration.getInstance().setPlayerType(PlayerType.CLIENT);

        GameScreens.showJoinGameScreen();
    }

    @FXML
    private void returnToPreviousScreen() {
        GameScreens.showChooseModeScreen();
    }
}