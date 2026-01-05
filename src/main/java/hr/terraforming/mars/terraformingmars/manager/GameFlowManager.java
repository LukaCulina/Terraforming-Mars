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

    private GameManager getGameManager() {
        return controller.getGameManager();
    }

    private GameBoard getGameBoard() {
        return controller.getGameBoard();
    }

    public void startProductionPhase() {
        log.info("All players have passed. Starting Production Phase.");

        getGameManager().doProduction();

        controller.updateAllUI();

        if (getGameBoard().isFinalGeneration()) {
            log.info("This was the last generation. Starting final greenery conversion phase.");

            getGameManager().resetDraftPhase();

            finalGreeneryManager = new FinalGreeneryPhaseManager(
                    getGameManager(),
                    controller.getSceneWindow(),
                    controller,
                    this::finishGame
            );
            finalGreeneryManager.start();
        }
        else {
            startNewGeneration();
        }
    }

    private void finishGame() {
        log.info("Final Greenery phase complete. Calculating final scores.");

        List<Player> rankedPlayers = getGameManager().calculateFinalScores();

        Platform.runLater(() -> ScreenNavigator.showGameOverScreen(rankedPlayers));

        var config = ApplicationConfiguration.getInstance();

        if (config.getPlayerType() == PlayerType.HOST && config.getBroadcaster() != null) {
            config.getBroadcaster().broadcast();
            log.info("HOST broadcasted final GameState after scoring");
        }
    }

    private void startNewGeneration() {
        log.info("Production phase is over. Starting a new generation.");

        getGameManager().startNewGeneration();
        getGameManager().resetDraftPhase();

        controller.setViewedPlayer(getGameManager().getCurrentPlayer());
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
                        getGameManager(),
                        controller.getSceneWindow(),
                        controller,
                        this::finishResearchPhase
                ).start();
            }
            default -> log.warn("Unknown or null PlayerType in startNewGeneration(): {}", playerType);
        }
    }

    public void finishResearchPhase() {
        if (getGameManager().getCurrentPhase() == GamePhase.ACTIONS) {
            log.warn("Already in ACTIONS phase, skipping duplicate beginActionPhase()");
            return;
        }

        saveResearchPhaseSnapshot(getGameManager());

        log.info("Research phase complete. Starting Action Phase.");

        getGameManager().beginActionPhase();

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

    private void saveResearchPhaseSnapshot(GameManager gameManager) {
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