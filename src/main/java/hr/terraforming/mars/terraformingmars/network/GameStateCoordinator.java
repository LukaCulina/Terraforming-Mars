package hr.terraforming.mars.terraformingmars.network;

import hr.terraforming.mars.terraformingmars.enums.GameplayPhase;
import hr.terraforming.mars.terraformingmars.model.ApplicationConfiguration;
import hr.terraforming.mars.terraformingmars.model.GameState;
import hr.terraforming.mars.terraformingmars.model.Player;
import hr.terraforming.mars.terraformingmars.view.GameScreens;
import javafx.application.Platform;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class GameStateCoordinator implements GameStateListener {

    private GameplayPhase currentPhase = GameplayPhase.JOINING;
    private final GameClientThread client;
    private boolean nameSent = false;
    private static final AtomicInteger callCount = new AtomicInteger(0);

    public GameStateCoordinator(GameClientThread client) {
        this.client = client;
    }

    @Override
    public void onGameStateReceived(GameState state) {
        int callId = callCount.incrementAndGet();
        log.info("🎯 CALL #{} from THREAD: {}", callId, Thread.currentThread().getName());

        Platform.runLater(() -> {
            log.info("🎯 PLATFORM CALL #{} - START", callId);

            String myPlayerName = ApplicationConfiguration.getInstance().getMyPlayerName();

            switch (currentPhase) {
                case JOINING -> handleJoiningPhase(state, myPlayerName);
                case CORPORATION_SELECTION -> handleCorporationPhase(state);
                case CARD_DRAFT -> handleCardDraftPhase(state);
                case PLAYING -> handlePlayingPhase(state);
            }
            log.info("🎯 PLATFORM CALL #{} - END", callId);

        });
    }

    private void handleJoiningPhase(GameState state, String myPlayerName) {
        if (!nameSent) {
            client.sendPlayerName(myPlayerName);
            nameSent = true;
        }

        boolean allJoined = state.gameManager().getPlayers().stream()
                .noneMatch(p -> p.getName().startsWith("Player "));

        if (allJoined) {
            currentPhase = GameplayPhase.CORPORATION_SELECTION;
            GameScreens.showChooseCorporationScreen(state.gameManager());
        }
    }

    private void handleCorporationPhase(GameState state) {
        boolean allChosen = state.gameManager().getPlayers().stream()
                .allMatch(p -> p.getCorporation() != null);

        if (allChosen) {
            currentPhase = GameplayPhase.CARD_DRAFT;
            GameScreens.showInitialCardDraftScreen(state.gameManager());
            return;
        }
        Player currentPlayer = state.gameManager().getCurrentPlayer();

        if (currentPlayer.getCorporation() == null) {
            GameScreens.showChooseCorporationScreen(state.gameManager());
        }
    }

    private void handleCardDraftPhase(GameState state) {
        Player currentPlayerForDraft = state.gameManager().getCurrentPlayerForDraft();

        if (currentPlayerForDraft == null) {
            currentPhase = GameplayPhase.PLAYING;

            GameScreens.startGameWithChosenCards(state);
            return;
        }

        GameScreens.showInitialCardDraftScreen(state.gameManager());
    }

    private void handlePlayingPhase(GameState state) {
        var controller = ApplicationConfiguration.getInstance().getActiveGameController();
        if (controller != null) {
            controller.updateFromNetwork(state);
        } else {
            log.warn("CLIENT: Received PLAYING phase state, but controller is not set!");
        }
    }
}