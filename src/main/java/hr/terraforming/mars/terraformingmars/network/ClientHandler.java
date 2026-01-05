package hr.terraforming.mars.terraformingmars.network;

import hr.terraforming.mars.terraformingmars.manager.ActionManager;
import hr.terraforming.mars.terraformingmars.model.*;
import hr.terraforming.mars.terraformingmars.network.message.CardChoiceMessage;
import hr.terraforming.mars.terraformingmars.network.message.CorporationChoiceMessage;
import hr.terraforming.mars.terraformingmars.network.message.PlayerNameMessage;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class ClientHandler implements Runnable {
    private final Socket socket;
    private final GameManager gameManager;

    private ObjectOutputStream clientOutput;
    private ObjectInputStream clientInput;
    private volatile boolean isClientReady = false;
    private volatile boolean isServerRunning = true;
    @Getter private String playerName;
    private ClientMessageHandler messageHandler;
    private final CompletableFuture<Void> readyFuture = new CompletableFuture<>();

    public ClientHandler(Socket socket, GameManager gameManager, ActionManager actionManager) {
        this.socket = socket;
        this.gameManager = gameManager;
        setActionManager(actionManager);
    }

    public void setActionManager(ActionManager actionManager) {
       messageHandler = new ClientMessageHandler(
                gameManager,
                actionManager,
                this::broadcastIfAvailable
        );
    }

    @Override
    public void run() {
        try (ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream())) {

            clientOutput = outputStream;
            clientInput = inputStream;
            isClientReady = true;
            readyFuture.complete(null);

            while (isServerRunning && !socket.isClosed()) {
                Object obj = inputStream.readObject();
                handleMessage(obj);
            }

        } catch (IOException | ClassNotFoundException e) {
            if (isServerRunning) {
                log.error("Client handler error", e);
            } else {
                log.info("Client disconnected gracefully");
            }
        } finally {
            cleanup();
        }
    }

    private void handleMessage(Object obj) {
        switch (obj) {
            case PlayerNameMessage msg -> playerName = messageHandler.handlePlayerName(msg);
            case CorporationChoiceMessage msg -> messageHandler.handleCorporationChoice(playerName, msg);
            case CardChoiceMessage msg -> messageHandler.handleCardChoice(playerName, msg);
            case GameMove move -> messageHandler.handleGameMove(move);
            default -> log.warn("Unknown message type: {}", obj.getClass());
        }
    }

    private void broadcastIfAvailable() {
        NetworkBroadcaster broadcaster = ApplicationConfiguration.getInstance().getBroadcaster();
        if (broadcaster != null) {
            broadcaster.broadcast();
        }
    }

    public boolean waitUntilReady(long timeoutMillis) {
        try {
            readyFuture.get(timeoutMillis, java.util.concurrent.TimeUnit.MILLISECONDS);
            return true;
        } catch (Exception _) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    public synchronized void sendGameState(GameState state) {
        log.debug("Sending GameState to {}", playerName);

        if (!isClientReady) { return; }

        try {
            clientOutput.writeObject(state);
            clientOutput.reset();
            clientOutput.flush();
        } catch (IOException e) {
            log.error("Failed to send game state", e);
            cleanup();
        }
    }

    public synchronized void sendObject(Object message) {
        if (!isClientReady) return;
        try {
            clientOutput.writeObject(message);
            clientOutput.reset();
            clientOutput.flush();
            log.debug("Sent object of type {} to {}", message.getClass().getSimpleName(), playerName);
        } catch (IOException e) {
            log.error("Failed to send object to {}", playerName, e);
            cleanup();
        }
    }

    public void close() {
        cleanup();
    }

    private void cleanup() {
        isServerRunning = false;
        isClientReady = false;
        try {
            if (clientInput != null) clientInput.close();
            if (clientOutput != null) clientOutput.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            log.error("Error closing client resources", e);
        }
    }
}
