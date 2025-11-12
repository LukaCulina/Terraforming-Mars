package hr.terraforming.mars.terraformingmars.controller;

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
        int numberOfPlayers;
        Button btn = (Button) event.getSource();

        try {
            numberOfPlayers = Integer.parseInt(btn.getText());

            log.info("Number of players selected: {}", numberOfPlayers);

            List<Player> players = new ArrayList<>();
            for (int i = 1; i <= numberOfPlayers; i++) {
                players.add(new Player("Player " + i, i));
            }

            GameBoard gameBoard = new GameBoard();
            GameManager gameManager = new GameManager(players, gameBoard);

            GameScreens.showChooseCorporationScreen(gameManager);


        } catch (NumberFormatException e) {
            log.error("Failed to parse number of players from button text: '{}'", btn.getText(), e);
        }
    }
}
