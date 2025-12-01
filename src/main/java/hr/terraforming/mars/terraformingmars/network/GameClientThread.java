package hr.terraforming.mars.terraformingmars.network;

import hr.terraforming.mars.terraformingmars.model.Card;
import hr.terraforming.mars.terraformingmars.model.GameMove;
import hr.terraforming.mars.terraformingmars.model.GameState;
import javafx.application.Platform;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class GameClientThread implements Runnable {
    private final String hostname;
    private final int port;
    private Socket socket;
    private ObjectOutputStream out;
    private final List<GameStateListener> listeners = new ArrayList<>();
    private volatile boolean running = true;

    public GameClientThread(String hostname, int port) {
        this.hostname = hostname;
        this.port = port;
    }

    @Override
    public void run() {
        try (Socket clientSocket = new Socket(hostname, port);
             ObjectOutputStream outputStream = new ObjectOutputStream(clientSocket.getOutputStream());
             ObjectInputStream inputStream = new ObjectInputStream(clientSocket.getInputStream())) {

            this.socket = clientSocket;
            this.out = outputStream;

            log.info("Connected to server at {}:{}", hostname, port);

            while (running) {
                GameState state = (GameState) inputStream.readObject();
                log.info("Received game state update");

                Platform.runLater(() -> {
                    synchronized (listeners) {
                        for (GameStateListener listener : listeners) {
                            listener.onGameStateReceived(state);
                        }
                    }
                });
            }

        } catch (IOException | ClassNotFoundException e) {
            if (running) {
                log.error("Client error", e);
            } else {
                log.info("Client disconnected");
            }
        }
    }

    public void addGameStateListener(GameStateListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    public void sendCardChoice(List<Card> selectedCards) {
        try {
            if (out != null) {
                out.writeObject(new CardChoiceMessage(
                        selectedCards.stream().map(Card::getName).toList()
                ));
                out.flush();
                log.info("✅ Sent card choice with {} cards", selectedCards.size());
            }
        } catch (IOException e) {
            log.error("Failed to send card choice", e);
        }
    }

    public void sendPlayerName(String playerName) {
        try {
            if (out != null) {
                out.writeObject(new PlayerNameMessage(playerName));
                out.flush();
                log.info("✅ Sent player name to server: {}", playerName);
            }
        } catch (IOException e) {
            log.error("Failed to send player name", e);
        }
    }

    public void sendCorporationChoice(String corporationName) {
        try {
            if (out != null) {
                out.writeObject(new CorporationChoiceMessage(corporationName));
                out.flush();
                log.info("✅ Sent corporation choice to server: {}", corporationName);
            }
        } catch (IOException e) {
            log.error("Failed to send corporation choice", e);
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

    public void shutdown() {
        running = false;
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            log.error("Error closing client socket", e);
        }
    }
}
