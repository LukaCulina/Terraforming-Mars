package hr.terraforming.mars.terraformingmars.network;

import hr.terraforming.mars.terraformingmars.model.*;
import hr.terraforming.mars.terraformingmars.network.message.*;
import lombok.extern.slf4j.Slf4j;

import java.io.EOFException;
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
    private GameState lastGameState;
    private final PlayerMessageHandler messageDispatcher;

    public GameClientThread(String hostname, int port) {
        this.hostname = hostname;
        this.port = port;
        this.messageDispatcher = new PlayerMessageHandler(this, listeners);
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
                Object receivedObject = inputStream.readObject();
                processMessage(receivedObject);
            }

        } catch (EOFException _) {
            log.info("Server closed connection");
        } catch (IOException | ClassNotFoundException e) {
            handleConnectionError(e);
        }
    }

    private void processMessage(Object message) {
        if (message instanceof GameState state) {
            this.lastGameState = state;
        }
        messageDispatcher.dispatch(message, lastGameState);
    }

    private void handleConnectionError(Exception e) {
        if (running) {
            log.error("Client error", e);
        } else {
            log.info("Client disconnected");
        }
    }

    public void addGameStateListener(GameStateListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    public void sendCardChoice(List<Card> selectedCards) {
        sendMessage(
                new CardChoiceMessage(selectedCards.stream().map(Card::getName).toList()),
                () -> log.debug("Sent card choice with {} cards", selectedCards.size())
        );
    }

    public void sendPlayerName(String playerName) {
        sendMessage(
                new PlayerNameMessage(playerName),
                () -> log.debug("Sent player name to server: {}", playerName)
        );
    }

    public void sendCorporationChoice(String corporationName) {
        sendMessage(
                new CorporationChoiceMessage(corporationName),
                () -> log.debug("Sent corporation choice to server: {}", corporationName)
        );
    }

    public void sendMove(GameMove move) {
        sendMessage(move, null);
    }

    private void sendMessage(Object message, Runnable onSuccess) {
        try {
            if (out != null) {
                out.writeObject(message);
                out.flush();
                if (onSuccess != null) {
                    onSuccess.run();
                }
            }
        } catch (IOException e) {
            log.error("Failed to send message: {}", message.getClass().getSimpleName(), e);
        }
    }

    public void shutdown() {
        log.info("GameClientThread shutting down");
        running = false;

        closeSocket();
        clearState();
    }

    private void closeSocket() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            log.error("Error closing client socket", e);
        }
    }

    private void clearState() {
        synchronized (listeners) {
            listeners.clear();
        }
        lastGameState = null;
        out = null;
    }
}
