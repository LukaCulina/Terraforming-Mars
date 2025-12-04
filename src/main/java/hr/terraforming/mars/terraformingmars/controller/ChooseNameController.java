package hr.terraforming.mars.terraformingmars.controller;

import hr.terraforming.mars.terraformingmars.model.ApplicationConfiguration;
import hr.terraforming.mars.terraformingmars.model.GameBoard;
import hr.terraforming.mars.terraformingmars.model.GameManager;
import hr.terraforming.mars.terraformingmars.model.Player;
import hr.terraforming.mars.terraformingmars.network.GameServerThread;
import hr.terraforming.mars.terraformingmars.network.HostGameStateCoordinator;
import hr.terraforming.mars.terraformingmars.network.NetworkBroadcaster;
import hr.terraforming.mars.terraformingmars.view.ScreenNavigator;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ChooseNameController {
    @FXML
    private TextField nameInput;

    private GameManager gameManager;
    private GameBoard gameBoard;

    public void setup(GameManager gameManager, GameBoard gameBoard) {
        this.gameManager = gameManager;
        this.gameBoard = gameBoard;
    }

    @FXML
    private void handleContinue() {
        String name = nameInput.getText().trim();

        if (name.isEmpty()) {
            nameInput.setPromptText("Please enter your name!");
            return;
        }

        log.info("Host entered name: {}", name);

        ApplicationConfiguration.getInstance().setMyPlayerName(name);

        Player player1 = gameManager.getPlayers().getFirst();
        player1.setName(name);

        startServerInBackground(gameManager, gameBoard);

        int playerCount = ApplicationConfiguration.getInstance().getPlayerCount();
        ScreenNavigator.showWaitingForPlayersScreen(gameManager, playerCount);
    }


    @FXML
    private void handleBack() {
        ScreenNavigator.showChoosePlayersScreen();
    }

    private void startServerInBackground(GameManager gameManager, GameBoard gameBoard) {
        int playerCount = ApplicationConfiguration.getInstance().getPlayerCount();

        new Thread(() -> {
            try {
                log.info("Starting server on port 1234...");

                GameServerThread gameServer = new GameServerThread(
                        gameManager,
                        gameBoard,
                        null,
                        playerCount
                );
                NetworkBroadcaster broadcaster = new NetworkBroadcaster(gameManager, gameBoard);
                ApplicationConfiguration.getInstance().setBroadcaster(broadcaster);

                HostGameStateCoordinator hostCoordinator = new HostGameStateCoordinator();
                gameServer.addLocalListener(hostCoordinator);

                ApplicationConfiguration.getInstance().setGameServer(gameServer);

                gameServer.run();

            } catch (Exception e) {
                log.error("Failed to start server", e);
            }
        }).start();

        log.info("Server thread started, listening for {} clients", playerCount - 1);
    }
}