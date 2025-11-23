package hr.terraforming.mars.terraformingmars.controller;

import hr.terraforming.mars.terraformingmars.enums.PlayerType;
import hr.terraforming.mars.terraformingmars.model.ApplicationConfiguration;
import hr.terraforming.mars.terraformingmars.view.GameScreens;
import hr.terraforming.mars.terraformingmars.model.GameBoard;
import hr.terraforming.mars.terraformingmars.model.GameManager;
import hr.terraforming.mars.terraformingmars.model.Player;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ChoosePlayersController {

    @FXML
    private void selectPlayers(ActionEvent event) {
        Button btn = (Button) event.getSource();

        try {
            int numberOfPlayers = Integer.parseInt(btn.getText());

            log.info("Number of players selected: {}", numberOfPlayers);

            createGame(numberOfPlayers);

        } catch (NumberFormatException e) {
            log.error("Failed to parse number of players from button text: '{}'", btn.getText(), e);
        }
    }

    private void createGame(int numberOfPlayers) {

        ApplicationConfiguration config = ApplicationConfiguration.getInstance();
        config.setPlayerCount(numberOfPlayers);

        PlayerType playerType = ApplicationConfiguration.getInstance().getPlayerType();

        log.info("Creating {} game with {} players", playerType, numberOfPlayers);  // ✅ Jedan log

        List<Player> players = new ArrayList<>();
        for (int i = 1; i <= numberOfPlayers; i++) {
            players.add(new Player("Player " + i, i));
        }

        GameBoard gameBoard = new GameBoard();
        GameManager gameManager = new GameManager(players, gameBoard);

        if (playerType == PlayerType.LOCAL) {
            GameScreens.showChooseCorporationScreen(gameManager);
        } else if (playerType == PlayerType.HOST) {
            GameScreens.showChooseNameScreen(gameManager, gameBoard);
        }
    }

    @FXML
    private void returnToPreviousScreen() {
        PlayerType playerType = ApplicationConfiguration.getInstance().getPlayerType();

        if (playerType == PlayerType.LOCAL) {
            GameScreens.showChooseModeScreen();
        } else if (playerType == PlayerType.HOST) {
            GameScreens.showChooseOnlineModeScreen();
        }
    }
}