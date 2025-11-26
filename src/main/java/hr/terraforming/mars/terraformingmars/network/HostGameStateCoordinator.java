package hr.terraforming.mars.terraformingmars.network;

import hr.terraforming.mars.terraformingmars.enums.GameplayPhase;
import hr.terraforming.mars.terraformingmars.model.ApplicationConfiguration;
import hr.terraforming.mars.terraformingmars.model.GameState;
import hr.terraforming.mars.terraformingmars.model.Player;
import hr.terraforming.mars.terraformingmars.view.GameScreens;
import javafx.application.Platform;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HostGameStateCoordinator implements GameStateListener {

    private GameplayPhase currentPhase = GameplayPhase.JOINING;

    @Override
    public void onGameStateReceived(GameState state) {
        Platform.runLater(() -> {

            switch (currentPhase) {
                case JOINING -> {
                    boolean allJoined = state.gameManager().getPlayers().stream()
                            .noneMatch(p -> p.getName().startsWith("Player "));

                    if (allJoined) {
                        log.info("HOST: Svi igrači spojeni, prelazim na Corporation Selection");
                        currentPhase = GameplayPhase.CORPORATION_SELECTION;
                        GameScreens.showChooseCorporationScreen(state.gameManager());
                    }
                }
                case CORPORATION_SELECTION -> {
                    boolean allChosen = state.gameManager().getPlayers().stream()
                            .allMatch(p -> p.getCorporation() != null);

                    if (allChosen) {
                        log.info("HOST: Svi odabrali korporacije, prelazim na Card Draft");
                        currentPhase = GameplayPhase.CARD_DRAFT;
                        GameScreens.showInitialCardDraftScreen(state.gameManager());
                        return;
                    }

                    GameScreens.showChooseCorporationScreen(state.gameManager());
                }
                case CARD_DRAFT -> {
                    Player currentPlayer = state.gameManager().getCurrentPlayerForDraft();
                    if (currentPlayer == null) {
                        log.info("HOST: Draft gotov, počinje igra");
                        currentPhase = GameplayPhase.PLAYING;
                        GameScreens.startGameWithChosenCards(state);
                    } else {
                        GameScreens.showInitialCardDraftScreen(state.gameManager());
                    }
                }
                case PLAYING -> {
                    var controller = ApplicationConfiguration.getInstance().getActiveGameController();
                    if (controller != null) {
                        controller.updateFromNetwork(state);
                    } else {
                        log.warn("HOST: Primio sam stanje u PLAYING fazi, ali kontroler nije postavljen!");
                    }
                }
            }
        });
    }
}