package hr.terraforming.mars.terraformingmars.network;

import hr.terraforming.mars.terraformingmars.manager.ActionManager;
import hr.terraforming.mars.terraformingmars.model.GameBoard;
import hr.terraforming.mars.terraformingmars.model.GameManager;
import hr.terraforming.mars.terraformingmars.model.GameMove;
import hr.terraforming.mars.terraformingmars.model.GameState;
import javafx.application.Platform;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

@Slf4j
public class ClientHandler implements Runnable {
    private final Socket socket;
    private final GameManager gameManager;
    private final GameBoard gameBoard;
    private final GameServerThread server;
    private final ActionManager actionManager;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    public ClientHandler(Socket socket, GameManager gameManager,GameBoard gameBoard, GameServerThread server, ActionManager actionManager) {
        this.socket = socket;
        this.gameManager = gameManager;
        this.gameBoard = gameBoard;
        this.server = server;
        this.actionManager = actionManager;
    }

    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            // Send initial game state
            sendGameState(new GameState(gameManager, gameBoard));

            // Listen for client moves
            while (true) {
                GameMove move = (GameMove) in.readObject();
                log.info("Received move from client: {}", move);

                // Process move on server
                Platform.runLater(() -> {
                    actionManager.processMove(move);

                    // Broadcast new state to all clients
                    GameState newState = new GameState(gameManager, gameBoard);
                    server.broadcastGameState(newState);
                });
            }

        } catch (IOException | ClassNotFoundException e) {
            log.error("Client handler error", e);
        }
    }

    public void sendGameState(GameState state) {
        try {
            out.writeObject(state);
            out.flush();
        } catch (IOException e) {
            log.error("Failed to send game state", e);
        }
    }

    public void close() {
        try {
            if (socket != null) socket.close();
        } catch (IOException e) {
            log.error("Error closing client", e);
        }
    }
}
