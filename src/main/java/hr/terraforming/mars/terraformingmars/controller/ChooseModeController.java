package hr.terraforming.mars.terraformingmars.controller;

import hr.terraforming.mars.terraformingmars.enums.PlayerType;
import hr.terraforming.mars.terraformingmars.model.ApplicationConfiguration;
import hr.terraforming.mars.terraformingmars.view.ScreenNavigator;
import javafx.fxml.FXML;

public class ChooseModeController {

    @FXML
    private void handleLocalMode() {
        ApplicationConfiguration.getInstance().setPlayerType(PlayerType.LOCAL);

        ScreenNavigator.showChoosePlayersScreen();
    }

    @FXML
    private void handleOnlineMode() {
        ApplicationConfiguration.getInstance().setPlayerType(PlayerType.HOST);

        ScreenNavigator.showChooseOnlineModeScreen();
    }
}
