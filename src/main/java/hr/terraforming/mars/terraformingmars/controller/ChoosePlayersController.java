package hr.terraforming.mars.terraformingmars.controller;

import hr.terraforming.mars.terraformingmars.view.GameScreens;
import hr.terraforming.mars.terraformingmars.model.GameBoard;
import hr.terraforming.mars.terraformingmars.model.GameManager;
import hr.terraforming.mars.terraformingmars.model.Player;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class ChoosePlayersController {

    private static final Logger logger = LoggerFactory.getLogger(ChoosePlayersController.class);


    @FXML
    private void selectPlayers(ActionEvent event) {
        int numberOfPlayers;
        Button btn = (Button) event.getSource();

        try {
            numberOfPlayers = Integer.parseInt(btn.getText());

            logger.info("Number of players selected: {}", numberOfPlayers);

            List<Player> players = new ArrayList<>();
            for (int i = 1; i <= numberOfPlayers; i++) {
                players.add(new Player("Player " + i, i));
            }

            GameBoard gameBoard = new GameBoard();
            GameManager gameManager = new GameManager(players, gameBoard);

            GameScreens.showChooseCorporationScreen(gameManager);


        } catch (NumberFormatException e) {
            logger.error("Failed to parse number of players from button text: '{}'", btn.getText(), e);
        }
    }
}
