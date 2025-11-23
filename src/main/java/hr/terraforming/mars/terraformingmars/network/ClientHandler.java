package hr.terraforming.mars.terraformingmars.network;

import hr.terraforming.mars.terraformingmars.factory.CardFactory;
import hr.terraforming.mars.terraformingmars.factory.CorporationFactory;
import hr.terraforming.mars.terraformingmars.manager.ActionManager;
import hr.terraforming.mars.terraformingmars.model.*;
import hr.terraforming.mars.terraformingmars.util.GameMoveUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class ClientHandler implements Runnable {
    private final Socket socket;
    private final GameManager gameManager;
    private final GameBoard gameBoard;
    private final GameServerThread server;
    private final ActionManager actionManager;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private volatile boolean ready = false;
    private final CompletableFuture<Void> readyFuture = new CompletableFuture<>();
    private volatile boolean running = true;

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

            ready = true;
            readyFuture.complete(null);
            log.info("✅ ClientHandler ready to send/receive");

            while (running && !socket.isClosed()) {
                Object obj = in.readObject();
                handleMessage(obj);
            }

            log.info("ClientHandler stopped gracefully");

        } catch (IOException | ClassNotFoundException e) {
            if (running) {
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
            case PlayerNameMessage msg -> handlePlayerName(msg);
            case CorporationChoiceMessage msg -> handleCorporationChoice(msg);
            case CardChoiceMessage msg -> handleCardChoice(msg);
            case GameMove move -> handleGameMove(move);
            default -> log.warn("Unknown message type: {}", obj.getClass());
        }
    }

    private void handlePlayerName(PlayerNameMessage msg) {
        log.info("✅ Received player name from client: {}", msg.playerName());

        for (Player p : gameManager.getPlayers()) {
            if (p.getName().startsWith("Player ")) {
                p.setName(msg.playerName());
                log.info("✅ Assigned name '{}' to player", msg.playerName());

                server.broadcastGameState(new GameState(gameManager, gameBoard));
                break;
            }
        }
    }

    private void handleCorporationChoice(CorporationChoiceMessage msg) {
        log.info("✅ Received corporation choice from client: {}", msg.corporationName());

        Player currentPlayer = gameManager.getCurrentPlayer();
        Corporation corp = CorporationFactory.getCorporationByName(msg.corporationName());

        if (corp != null) {
            gameManager.assignCorporationAndAdvance(corp);
            log.info("✅ Assigned corporation '{}' to {}", msg.corporationName(), currentPlayer.getName());
            server.broadcastGameState(new GameState(gameManager, gameBoard));
        } else {
            log.warn("Corporation not found: {}", msg.corporationName());
        }
    }

    private void handleCardChoice(CardChoiceMessage msg) {
        log.info("✅ Received card choice from client: {} cards", msg.cardNames().size());

        Player draftPlayer = gameManager.getCurrentPlayerForDraft();
        if (draftPlayer == null) {
            log.warn("No current draft player, ignoring card choice");
            return;
        }

        List<Card> boughtCards = msg.cardNames().stream()
                .map(CardFactory::getCardByName)
                .filter(Objects::nonNull)
                .toList();

        int cost = boughtCards.size() * 3;
        draftPlayer.spendMC(cost);
        draftPlayer.getHand().addAll(boughtCards);

        boolean morePlayers = gameManager.advanceDraftPlayer();
        if (!morePlayers) {
            GameMoveUtils.saveInitialSetupMove(gameManager);
            gameManager.startGame();
        }

        server.broadcastGameState(new GameState(gameManager, gameBoard));
        log.info("✅ Applied card choice for {} and broadcasted state", draftPlayer.getName());
    }

    private void handleGameMove(GameMove move) {
        if (actionManager != null) {
            actionManager.processMove(move);
        } else {
            log.warn("ActionManager is null, cannot process move");
        }
    }

    public boolean waitUntilReady(long timeoutMillis) {
        try {
            readyFuture.get(timeoutMillis, java.util.concurrent.TimeUnit.MILLISECONDS);
            return true;
        } catch (Exception _) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted while waiting for ClientHandler to initialize");
            return false;
        }
    }

    public synchronized void sendGameState(GameState state) {
        log.debug("Sending game state to client with gameBoard = {}", state.gameBoard() != null ? "NOT NULL" : "NULL");

        if (!ready) {
            log.warn("ClientHandler not ready yet, skipping broadcast");
            return;
        }

        try {
            out.writeObject(state);
            out.reset();
            out.flush();
        } catch (IOException e) {
            log.error("Failed to send game state", e);
            cleanup();
        }
    }

    private void cleanup() {
        running = false;
        ready = false;
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null && !socket.isClosed()) socket.close();
            log.info("ClientHandler resources cleaned up");
        } catch (IOException e) {
            log.error("Error closing client resources", e);
        }
    }

    public void close() {
        cleanup();
    }
}
