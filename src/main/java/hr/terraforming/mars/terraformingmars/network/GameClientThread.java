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
    private Socket clientSocket;
    private ObjectOutputStream serverOutput;
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
        try (Socket socket = new Socket(hostname, port);
             ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream())) {

            this.clientSocket = socket;
            this.serverOutput = outputStream;

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
            lastGameState = state;
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

    private synchronized void sendMessage(Object message, Runnable onSuccess) {
        try {
            if (serverOutput != null) {
                serverOutput.writeObject(message);
                serverOutput.flush();
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
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close();
            }
        } catch (IOException e) {
            log.error("Error closing client clientSocket", e);
        }
    }

    private void clearState() {
        synchronized (listeners) {
            listeners.clear();
        }
        lastGameState = null;
        serverOutput = null;
    }
}
