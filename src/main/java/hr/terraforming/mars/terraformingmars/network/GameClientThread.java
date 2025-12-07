package hr.terraforming.mars.terraformingmars.network;

import hr.terraforming.mars.terraformingmars.controller.ChooseCardsController;
import hr.terraforming.mars.terraformingmars.factory.CardFactory;
import hr.terraforming.mars.terraformingmars.factory.CorporationFactory;
import hr.terraforming.mars.terraformingmars.model.*;
import hr.terraforming.mars.terraformingmars.util.ScreenLoader;
import hr.terraforming.mars.terraformingmars.view.ScreenNavigator;
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
    private GameState lastGameState;

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
                Object receivedObject = inputStream.readObject();

                if (receivedObject instanceof GameState state) {
                    log.info("Received game state update");
                    this.lastGameState = state;

                    Platform.runLater(() -> {
                        synchronized (listeners) {
                            for (GameStateListener listener : listeners) listener.onGameStateReceived(state);
                        }
                    });
                }
                else if (receivedObject instanceof CorporationOfferMessage msg) {
                    Platform.runLater(() -> handleCorporationOffer(msg));
                }
                else if (receivedObject instanceof InitialCardsOfferMessage msg) {
                    Platform.runLater(() -> handleInitialCardsOffer(msg));
                }
            }

        } catch (IOException | ClassNotFoundException e) {
            if (running) {
                log.error("Client error", e);
            } else {
                log.info("Client disconnected");
            }
        }
    }

    private void handleCorporationOffer(CorporationOfferMessage msg) {
        String myName = ApplicationConfiguration.getInstance().getMyPlayerName();
        if (!myName.equals(msg.playerName())) return;

        List<Corporation> offer = msg.corporationNames().stream()
                .map(CorporationFactory::getCorporationByName)
                .toList();

        if (lastGameState == null) {
            return;
        }
        GameManager gm = lastGameState.gameManager();
        Player me = gm.getPlayerByName(myName);

        ScreenNavigator.showChooseCorporationScreen(me, offer, gm);
    }

    private void handleInitialCardsOffer(InitialCardsOfferMessage msg) {
        String myName = ApplicationConfiguration.getInstance().getMyPlayerName();
        if (!myName.equals(msg.playerName())) return;

        List<Card> offer = msg.cardNames().stream()
                .map(CardFactory::getCardByName)
                .toList();

        if (lastGameState == null) {
            return;
        }
        GameManager gm = lastGameState.gameManager();
        Player me = gm.getPlayerByName(myName);

        boolean isResearch = gm.getGeneration() > 1 || offer.size() <= 4;

        Platform.runLater(() -> {
            if (isResearch) {
                ScreenLoader.showAsModal(
                        ScreenNavigator.getMainStage(),
                        "ChooseCards.fxml",
                        "Research Phase",
                        0.7, 0.8,
                        (ChooseCardsController c) ->
                                c.setup(me, offer, null, gm, true)
                );
            } else {
                ScreenNavigator.showInitialCardDraftScreen(me, offer, gm);
            }
        });
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
