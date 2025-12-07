package hr.terraforming.mars.terraformingmars.network;

import hr.terraforming.mars.terraformingmars.jndi.ConfigurationKey;
import hr.terraforming.mars.terraformingmars.jndi.ConfigurationReader;
import hr.terraforming.mars.terraformingmars.manager.ActionManager;
import hr.terraforming.mars.terraformingmars.model.*;
import javafx.application.Platform;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

@Slf4j
public class GameServerThread implements Runnable {

    private final GameManager gameManager;
    private final GameBoard gameBoard;
    private ActionManager actionManager;
    private final int maxPlayers;
    private final List<ClientHandler> connectedClients = new CopyOnWriteArrayList<>();
    @Setter private Consumer<Integer> onPlayerCountChanged;
    private ServerSocket serverSocket;
    private final List<GameStateListener> localListeners = new CopyOnWriteArrayList<>();

    private CardDistributor cardDistributor;

    public GameServerThread(GameManager gameManager, GameBoard gameBoard, ActionManager actionManager, int maxPlayers) {
        this.gameManager = gameManager;
        this.gameBoard = gameBoard;
        this.actionManager = actionManager;
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

                ClientHandler handler = new ClientHandler(clientSocket, gameManager, actionManager);
                connectedClients.add(handler);
                new Thread(handler).start();

                if (handler.waitUntilReady(5000)) {
                    log.debug("ClientHandler ready");
                } else {
                    log.error("ClientHandler failed to initialize");
                }

                if (onPlayerCountChanged != null) {
                    Platform.runLater(() -> onPlayerCountChanged.accept(connectedClients.size()));
                }
                broadcastGameState(new GameState(gameManager, gameBoard));
            }
            log.info("All players connected, game can start!");

        } catch (IOException e) {
            log.error("Server error", e);
        }
    }

    public void sendToPlayer(String playerName, Object message) {
        for (ClientHandler client : connectedClients) {
            if (playerName.equals(client.getPlayerName())) {
                client.sendObject(message);
                return;
            }
        }
        log.warn("Cannot send message to player {}, client not found.", playerName);
    }

    public void distributeInitialCorporations() {
        ensureDistributor();
        cardDistributor.distributeInitialCorporations();
    }

    public void distributeInitialCards() {
        ensureDistributor();
        cardDistributor.distributeInitialCards();
    }

    public void distributeResearchCards() {
        ensureDistributor();
        cardDistributor.distributeResearchCards();
    }

    private void ensureDistributor() {
        if (cardDistributor == null) {
            this.cardDistributor = new CardDistributor(gameManager, this, actionManager);
        }
    }

    public void setActionManager(ActionManager actionManager) {
        this.actionManager = actionManager;
        log.info("ActionManager injected into GameServerThread");
        for (ClientHandler client : connectedClients) {
            client.setActionManager(actionManager);
        }
        this.cardDistributor = new CardDistributor(gameManager, this, actionManager);
    }

    public void addLocalListener(GameStateListener listener) {
        if (listener != null) {
            this.localListeners.add(listener);
        }
    }

    public void broadcastGameState(GameState state) {
        log.debug("Broadcasting to {} clients...", connectedClients.size());
        for (ClientHandler client : connectedClients) {
            client.sendGameState(state);
        }
        for (GameStateListener listener : localListeners) {
            Platform.runLater(() -> listener.onGameStateReceived(state));
        }
    }

    public void removeLocalListener(GameStateListener listener) {
        this.localListeners.remove(listener);
    }

    public void shutdown() {
        try {
            if (serverSocket != null) serverSocket.close();
            for (ClientHandler client : connectedClients) client.close();
        } catch (IOException e) {
            log.error("Error shutting down server", e);
        }
    }
}
