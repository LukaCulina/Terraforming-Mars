package hr.terraforming.mars.terraformingmars.controller;

import hr.terraforming.mars.terraformingmars.enums.PlayerType;
import hr.terraforming.mars.terraformingmars.view.GameScreens;
import javafx.fxml.FXML;

public class ChooseModeController {

    @FXML
    private void handleLocalMode() {
        // Postavi na LOCAL mode
        //ApplicationConfiguration.getInstance().setPlayerType(PlayerType.LOCAL);

        // Idi na ChoosePlayersScreen (kao i do sada)
        GameScreens.showChoosePlayersScreen();
    }

    @FXML
    private void handleOnlineMode() {
        // Postavi na ONLINE mode
        //ApplicationConfiguration.getInstance().setPlayerType(PlayerType.HOST);

        // Za sada isto kao LOCAL (kasnije ćeš dodati OnlineModeScreen)
        GameScreens.showChoosePlayersScreen();
    }
}
