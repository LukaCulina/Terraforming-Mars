package hr.terraforming.mars.terraformingmars.network;

import hr.terraforming.mars.terraformingmars.enums.GameplayPhase;
import hr.terraforming.mars.terraformingmars.model.ApplicationConfiguration;
import hr.terraforming.mars.terraformingmars.model.GameState;
import hr.terraforming.mars.terraformingmars.model.Player;
import hr.terraforming.mars.terraformingmars.view.ScreenNavigator;
import javafx.application.Platform;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HostGameStateCoordinator implements GameStateListener {

    private GameplayPhase currentPhase = GameplayPhase.JOINING;

    @Override
    public void onGameStateReceived(GameState state) {
        Platform.runLater(() -> {
            switch (currentPhase) {
                case JOINING -> handleJoiningPhase(state);
                case CORPORATION_SELECTION -> handleCorporationSelection(state);
                case CARD_DRAFT -> handleCardDraft(state);
                case PLAYING -> handlePlayingPhase(state);
            }
        });
    }

    private void handleJoiningPhase(GameState state) {
        boolean allJoined = state.gameManager().getPlayers().stream()
                .noneMatch(p -> p.getName().startsWith("Player "));

        if (allJoined) {
            log.info("Host: All players joined, transitioning to Corporation Selection");
            currentPhase = GameplayPhase.CORPORATION_SELECTION;
            ApplicationConfiguration.getInstance().getGameServer().distributeInitialCorporations();
        }
    }

    private void handleCorporationSelection(GameState state) {
        boolean allChosen = state.gameManager().getPlayers().stream()
                .allMatch(p -> p.getCorporation() != null);

        if (allChosen) {
            log.info("Host: All players chose corporations, transitioning to Card Draft");
            currentPhase = GameplayPhase.CARD_DRAFT;
            ApplicationConfiguration.getInstance().getGameServer().distributeInitialCards();
        }
    }

    private void handleCardDraft(GameState state) {

        Player currentPlayer = state.gameManager().getCurrentPlayerForDraft();
        if (currentPlayer == null) {
            log.info("Host: Draft completed, starting game");
            currentPhase = GameplayPhase.PLAYING;
            ScreenNavigator.startGameWithChosenCards(state);
        }
    }

    private void handlePlayingPhase(GameState state) {
        var controller = ApplicationConfiguration.getInstance().getActiveGameController();
        if (controller != null) {
            controller.updateFromNetwork(state);
        } else {
            log.warn("Host: Received PLAYING phase state, but controller is not set!");
        }
    }
}