package hr.terraforming.mars.terraformingmars.network;

import hr.terraforming.mars.terraformingmars.enums.ActionType;
import hr.terraforming.mars.terraformingmars.factory.CardFactory;
import hr.terraforming.mars.terraformingmars.factory.CorporationFactory;
import hr.terraforming.mars.terraformingmars.manager.ActionManager;
import hr.terraforming.mars.terraformingmars.model.*;
import hr.terraforming.mars.terraformingmars.util.GameMoveUtils;
import javafx.application.Platform;
import lombok.Setter;
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
    @Setter private ActionManager actionManager;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private volatile boolean ready = false;
    private final CompletableFuture<Void> readyFuture = new CompletableFuture<>();
    private volatile boolean running = true;

    public ClientHandler(Socket socket, GameManager gameManager, ActionManager actionManager) {
        this.socket = socket;
        this.gameManager = gameManager;
        this.actionManager = actionManager;
    }

    @Override
    public void run() {
        try (ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream())) {

            this.out = outputStream;
            this.in = inputStream;
            ready = true;
            readyFuture.complete(null);

            while (running && !socket.isClosed()) {
                Object obj = inputStream.readObject();
                handleMessage(obj);
            }

        } catch (IOException | ClassNotFoundException e) {
            if (running) {
                log.error("Client handler error", e);
            } else {
                log.info("Client disconnected gracefully");
            }
        }
        cleanup();
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
        for (Player p : gameManager.getPlayers()) {
            if (p.getName().startsWith("Player ")) {
                p.setName(msg.playerName());
                broadcastIfAvailable();
                break;
            }
        }
    }

    private void handleCorporationChoice(CorporationChoiceMessage msg) {
        Corporation corp = CorporationFactory.getCorporationByName(msg.corporationName());

        if (corp != null) {
            gameManager.assignCorporationAndAdvance(corp);
            broadcastIfAvailable();
        }
    }

    private void handleCardChoice(CardChoiceMessage msg) {
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

        boolean broadcastHandledExternally = false;

        if (!morePlayers) {
            if (gameManager.getGeneration() == 0) {
                GameMoveUtils.saveInitialSetupMove(gameManager);
                gameManager.startGame();
            } else {
                broadcastHandledExternally = true;
                Platform.runLater(() -> {
                    var flowManager = actionManager.getGameFlowManager();
                    flowManager.onResearchComplete();
                });
            }
        }

        if (!broadcastHandledExternally) {
            broadcastIfAvailable();
        }
    }

    private void handleGameMove(GameMove move) {
        if (actionManager != null) {
            Platform.runLater(() -> {
                try {
                    actionManager.processMove(move);
                    if (shouldBroadcastAfterMove(move.actionType())) {
                        broadcastIfAvailable();
                    }
                } catch (Exception e) {
                    log.error("Error processing move on FX thread", e);
                }
            });
        } else {
            log.warn("ActionManager is null, cannot process move");
        }
    }

    private void broadcastIfAvailable() {
        NetworkBroadcaster broadcaster = ApplicationConfiguration.getInstance().getBroadcaster();
        if (broadcaster != null) {
            broadcaster.broadcast();
        }
    }

    private boolean shouldBroadcastAfterMove(ActionType type) {
        return type == ActionType.PLACE_TILE || type == ActionType.SELL_PATENTS;
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
        log.debug("Sending game state to client with gameBoard = {}", state.gameBoard() != null ? "NOT NULL" : "NULL");

        if (!ready) { return; }

        try {
            out.writeObject(state);
            out.reset();
            out.flush();
        } catch (IOException e) {
            log.error("Failed to send game state", e);
            cleanup();
        }
    }

    public void close() {
        cleanup();
    }

    private void cleanup() {
        running = false;
        ready = false;
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            log.error("Error closing client resources", e);
        }
    }
}