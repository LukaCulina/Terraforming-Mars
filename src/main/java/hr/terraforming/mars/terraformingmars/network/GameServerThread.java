package hr.terraforming.mars.terraformingmars.network;

import hr.terraforming.mars.terraformingmars.jndi.ConfigurationKey;
import hr.terraforming.mars.terraformingmars.jndi.ConfigurationReader;
import hr.terraforming.mars.terraformingmars.model.GameBoard;
import hr.terraforming.mars.terraformingmars.model.GameManager;
import hr.terraforming.mars.terraformingmars.model.GameState;
import javafx.application.Platform;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Slf4j
public class GameServerThread implements Runnable{

    private final GameManager gameManager;
    private final GameBoard gameBoard;
    private final int maxPlayers;
    private final List<ClientHandler> connectedClients = new ArrayList<>();
    private Consumer<Integer> onPlayerCountChanged;
    private ServerSocket serverSocket;

    public GameServerThread(GameManager gameManager,GameBoard gameBoard, int maxPlayers) {
        this.gameManager = gameManager;
        this.gameBoard = gameBoard;
        this.maxPlayers = maxPlayers;
    }

    @Override
    public void run() {
        try {
            int port = ConfigurationReader.getIntegerValue(ConfigurationKey.SERVER_PORT);
            serverSocket = new ServerSocket(port);

            log.info("Server started on port {}, waiting for {} players...", port, maxPlayers - 1);

            while (connectedClients.size() < maxPlayers - 1) {
                Socket clientSocket = serverSocket.accept();
                log.info("Client connected: {}", clientSocket.getInetAddress());

                ClientHandler handler = new ClientHandler(clientSocket, gameManager,gameBoard, this);
                connectedClients.add(handler);
                new Thread(handler).start();

                // Notify controller
                if (onPlayerCountChanged != null) {
                    Platform.runLater(() -> onPlayerCountChanged.accept(connectedClients.size()));
                }
            }

            log.info("All players connected, game can start!");

        } catch (IOException e) {
            log.error("Server error", e);
        }
    }

    public void broadcastGameState(GameState state) {
        for (ClientHandler client : connectedClients) {
            client.sendGameState(state);
        }
    }

    public int getConnectedPlayerCount() {
        return connectedClients.size();
    }

    public void setOnPlayerCountChanged(Consumer<Integer> callback) {
        this.onPlayerCountChanged = callback;
    }

    public void shutdown() {
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
            for (ClientHandler client : connectedClients) {
                client.close();
            }
        } catch (IOException e) {
            log.error("Error shutting down server", e);
        }
    }

}
