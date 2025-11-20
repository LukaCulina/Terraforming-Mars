package hr.terraforming.mars.terraformingmars.network;

import hr.terraforming.mars.terraformingmars.model.GameMove;
import hr.terraforming.mars.terraformingmars.model.GameState;
import javafx.application.Platform;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.function.Consumer;

@Slf4j
public class GameClientThread implements Runnable {
    private final String hostname;
    private final int port;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private Consumer<GameState> onGameStateReceived;

    public GameClientThread(String hostname, int port) {
        this.hostname = hostname;
        this.port = port;
    }

    @Override
    public void run() {
        try {
            socket = new Socket(hostname, port);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            log.info("Connected to server at {}:{}", hostname, port);

            // Listen for game state updates from server
            while (true) {
                GameState state = (GameState) in.readObject();
                log.info("Received game state update");

                if (onGameStateReceived != null) {
                    Platform.runLater(() -> onGameStateReceived.accept(state));
                }
            }

        } catch (IOException | ClassNotFoundException e) {
            log.error("Client error", e);
        }
    }

    public void sendMove(GameMove move) {
        try {
            out.writeObject(move);
            out.flush();
        } catch (IOException e) {
            log.error("Failed to send move", e);
        }
    }

    public void setOnGameStateReceived(Consumer<GameState> callback) {
        this.onGameStateReceived = callback;
    }
}
