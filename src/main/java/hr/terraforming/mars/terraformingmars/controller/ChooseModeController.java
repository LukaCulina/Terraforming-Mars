package hr.terraforming.mars.terraformingmars.controller;

import hr.terraforming.mars.terraformingmars.enums.PlayerType;
import hr.terraforming.mars.terraformingmars.model.ApplicationConfiguration;
import hr.terraforming.mars.terraformingmars.view.GameScreens;
import javafx.fxml.FXML;

public class ChooseModeController {

    @FXML
    private void handleLocalMode() {
        ApplicationConfiguration.getInstance().setPlayerType(PlayerType.LOCAL);

        GameScreens.showChoosePlayersScreen();
    }

    @FXML
    private void handleOnlineMode() {
        ApplicationConfiguration.getInstance().setPlayerType(PlayerType.HOST);

        GameScreens.showChooseOnlineModeScreen();
    }
}
