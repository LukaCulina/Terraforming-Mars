package hr.terraforming.mars.terraformingmars.network;

import hr.terraforming.mars.terraformingmars.jndi.ConfigurationKey;
import hr.terraforming.mars.terraformingmars.jndi.ConfigurationReader;
import hr.terraforming.mars.terraformingmars.manager.ActionManager;
import hr.terraforming.mars.terraformingmars.model.GameBoard;
import hr.terraforming.mars.terraformingmars.model.GameManager;
import hr.terraforming.mars.terraformingmars.model.GameState;
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
public class GameServerThread implements Runnable{

    private final GameManager gameManager;
    private final GameBoard gameBoard;
    private ActionManager actionManager;
    private final int maxPlayers;
    private final List<ClientHandler> connectedClients = new CopyOnWriteArrayList<>();
    @Setter
    private Consumer<Integer> onPlayerCountChanged;
    private ServerSocket serverSocket;
    private final List<GameStateListener> localListeners = new CopyOnWriteArrayList<>();

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

                ClientHandler handler = new ClientHandler(clientSocket, gameManager,gameBoard, this, actionManager);
                connectedClients.add(handler);
                new Thread(handler).start();

                waitForHandlerReady(handler);

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

    private void waitForHandlerReady(ClientHandler handler) {
        boolean initialized = handler.waitUntilReady(5000);

        if (!initialized) {
            log.error("ClientHandler failed to initialize after 5000 ms");
        } else {
            log.debug("ClientHandler ready");
        }
    }

    public void setActionManager(ActionManager actionManager) {
        this.actionManager = actionManager;
        log.info("✅ ActionManager injected into GameServerThread");

        for (ClientHandler client : connectedClients) {
            client.setActionManager(actionManager);
        }
    }

    public void addLocalListener(GameStateListener listener) {
        if (listener != null) {
            this.localListeners.add(listener);
            log.info("🔌 Listener added. Total listeners: {}", localListeners.size());
        }
    }

    public void removeLocalListener(GameStateListener listener) {
        this.localListeners.remove(listener);
    }

    public void broadcastGameState(GameState state) {
        log.info("📡 Broadcasting to {} clients and {} local listeners",
                connectedClients.size(), localListeners.size());

        for (GameStateListener listener : localListeners) {
            log.info("   - Listener: {}", listener.getClass().getSimpleName());
        }

        for (ClientHandler client : connectedClients) {
            client.sendGameState(state);
        }

        for (GameStateListener listener : localListeners) {
            Platform.runLater(() -> {
                try {
                    log.info("🔔 Calling listener.onGameStateReceived()");
                    listener.onGameStateReceived(state);
                    log.info("✅ Listener callback completed");
                } catch (Exception e) {
                    log.error("❌ Error in local listener", e);
                }
            });
        }

        log.info("✅ Broadcast complete");
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
