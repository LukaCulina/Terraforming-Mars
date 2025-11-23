package hr.terraforming.mars.terraformingmars.network;

import hr.terraforming.mars.terraformingmars.model.ApplicationConfiguration;
import hr.terraforming.mars.terraformingmars.model.GameState;
import hr.terraforming.mars.terraformingmars.model.Player;
import hr.terraforming.mars.terraformingmars.view.GameScreens;
import javafx.application.Platform;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GameStateCoordinator implements GameStateListener {

    private enum GamePhase {
        JOINING,           // Čekanje da se svi spojе
        CORPORATION_SELECTION,  // Biranje korporacija
        CARD_DRAFT,        // Biranje karata
        PLAYING            // Glavna igra
    }

    private GamePhase currentPhase = GamePhase.JOINING;
    private final GameClientThread client;
    private boolean nameSent = false;

    public GameStateCoordinator(GameClientThread client) {
        this.client = client;
    }

    @Override
    public void onGameStateReceived(GameState state) {
        Platform.runLater(() -> {
            String myPlayerName = ApplicationConfiguration.getInstance().getMyPlayerName();

            switch (currentPhase) {
                case JOINING -> handleJoiningPhase(state, myPlayerName);
                case CORPORATION_SELECTION -> handleCorporationPhase(state);
                case CARD_DRAFT -> handleCardDraftPhase(state);
                case PLAYING -> handlePlayingPhase(state, myPlayerName);
            }
        });
    }

    private void handleJoiningPhase(GameState state, String myPlayerName) {
        if (!nameSent) {
            client.sendPlayerName(myPlayerName);
            nameSent = true;
            log.info("✅ Sent player name to server: {}", myPlayerName);
        }

        boolean allJoined = state.gameManager().getPlayers().stream()
                .noneMatch(p -> p.getName().startsWith("Player "));

        if (allJoined) {
            log.info("✅ All players joined! Going to Corporation Selection");
            currentPhase = GamePhase.CORPORATION_SELECTION;
            GameScreens.showChooseCorporationScreen(state.gameManager());
        }
    }

    private void handleCorporationPhase(GameState state) {
        boolean allChosen = state.gameManager().getPlayers().stream()
                .allMatch(p -> p.getCorporation() != null);

        if (allChosen) {
            log.info("✅ All players chose corporations! Going to card draft");
            currentPhase = GamePhase.CARD_DRAFT;
            GameScreens.showInitialCardDraftScreen(state.gameManager());
            return;
        }
        Player currentPlayer = state.gameManager().getCurrentPlayer();

        if (currentPlayer.getCorporation() == null) {
            log.info("✅ My turn to choose corporation!");
            GameScreens.showChooseCorporationScreen(state.gameManager());
        }
    }

    private void handleCardDraftPhase(GameState state) {
        Player currentPlayerForDraft = state.gameManager().getCurrentPlayerForDraft();

        if (currentPlayerForDraft == null) {
            log.info("✅ All players chose cards! Game started");
            currentPhase = GamePhase.PLAYING;

            GameScreens.startGameWithChosenCards(state);
            return;
        }

        GameScreens.showInitialCardDraftScreen(state.gameManager());
    }

    private void handlePlayingPhase(GameState state, String myPlayerName) {
        // Logika za glavnu igru...
    }
}