package hr.terraforming.mars.terraformingmars.manager;

import hr.terraforming.mars.terraformingmars.controller.game.GameScreenController;
import hr.terraforming.mars.terraformingmars.enums.ActionType;
import hr.terraforming.mars.terraformingmars.enums.GamePhase;
import hr.terraforming.mars.terraformingmars.enums.PlayerType;
import hr.terraforming.mars.terraformingmars.model.*;
import hr.terraforming.mars.terraformingmars.network.NetworkBroadcaster;
import hr.terraforming.mars.terraformingmars.util.XmlUtils;
import hr.terraforming.mars.terraformingmars.view.ScreenNavigator;
import javafx.application.Platform;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class GameFlowManager {

    private final GameScreenController controller;
    @Getter private FinalGreeneryPhaseManager finalGreeneryManager;

    public GameFlowManager(GameScreenController controller) {
        this.controller = controller;
    }

    private GameManager gm() {
        return controller.getGameManager();
    }

    private GameBoard board() {
        return controller.getGameBoard();
    }

    public void handleEndOfActionPhase() {
        log.info("All players have passed. Starting Production Phase.");

        gm().doProduction();

        controller.updateAllUI();

        if (board().isFinalGeneration()) {
            log.info("This was the last generation. Starting final greenery conversion phase.");

            gm().resetDraftPhase();

            this.finalGreeneryManager = new FinalGreeneryPhaseManager(
                    gm(),
                    controller.getSceneWindow(),
                    controller,
                    this::onFinalGreeneryComplete
            );
            finalGreeneryManager.start();
        }
        else {
            startNewGeneration();
        }
    }

    private void onFinalGreeneryComplete() {
        log.info("Final Greenery phase complete. Calculating final scores.");

        List<Player> rankedPlayers = gm().calculateFinalScores();

        Platform.runLater(() -> ScreenNavigator.showGameOverScreen(rankedPlayers));

        var cfg = ApplicationConfiguration.getInstance();
        if (cfg.getPlayerType() == PlayerType.HOST && cfg.getBroadcaster() != null) {
            cfg.getBroadcaster().broadcast();
            log.info("HOST broadcasted final GameState after scoring");
        }
    }

    private void startNewGeneration() {
        log.info("Production phase is over. Starting a new generation.");

        gm().startNewGeneration();
        gm().resetDraftPhase();

        controller.setViewedPlayer(gm().getCurrentPlayer());
        controller.updateAllUI();

        var config = ApplicationConfiguration.getInstance();
        var playerType = config.getPlayerType();

        switch (playerType) {
            case HOST -> {
                log.info("HOST: Starting distributed research phase.");
                config.getGameServer().distributeResearchCards();
            }
            case CLIENT -> log.info("CLIENT: Waiting for research cards from host...");
            case LOCAL -> {
                log.info("LOCAL: Starting local research phase manager.");
                new ResearchPhaseManager(
                        gm(),
                        controller.getSceneWindow(),
                        controller,
                        this::onResearchComplete
                ).start();
            }
            default -> log.warn("Unknown or null PlayerType in startNewGeneration(): {}", playerType);
        }
    }

    public void onResearchComplete() {
        if (gm().getCurrentPhase() == GamePhase.ACTIONS) {
            log.warn("Already in ACTIONS phase, skipping duplicate beginActionPhase()");
            return;
        }

        createAndSaveResearchPhaseSnapshot(gm());

        log.info("Research phase complete. Starting Action Phase.");

        gm().beginActionPhase();

        controller.updateAllUI();

        var config = ApplicationConfiguration.getInstance();
        if (config.getPlayerType() == hr.terraforming.mars.terraformingmars.enums.PlayerType.HOST) {
            NetworkBroadcaster broadcaster = config.getBroadcaster();
            if (broadcaster != null) {
                log.info("Broadcasting game state after research complete");
                broadcaster.broadcast();
            }
        }
    }

    private void createAndSaveResearchPhaseSnapshot(GameManager gameManager) {
        Map<String, Object> researchData = new HashMap<>();
        for (Player player : gameManager.getPlayers()) {
            Map<String, Object> playerData = new HashMap<>();
            playerData.put("hand", player.getHand().stream().map(Card::getName).toList());
            researchData.put(player.getName(), playerData);
        }

        String jsonDetails = new com.google.gson.Gson().toJson(researchData);

        GameMove researchMove = new GameMove(
                "System",
                ActionType.RESEARCH_COMPLETE,
                jsonDetails,
                LocalDateTime.now()
        );

        XmlUtils.appendGameMove(researchMove);
        log.debug("RESEARCH_PHASE_COMPLETE snapshot saved to XML.");
    }
}