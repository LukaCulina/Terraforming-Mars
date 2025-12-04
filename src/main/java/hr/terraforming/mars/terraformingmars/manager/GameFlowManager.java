package hr.terraforming.mars.terraformingmars.manager;

import hr.terraforming.mars.terraformingmars.enums.ActionType;
import hr.terraforming.mars.terraformingmars.enums.GamePhase;
import hr.terraforming.mars.terraformingmars.model.*;
import hr.terraforming.mars.terraformingmars.network.NetworkBroadcaster;
import hr.terraforming.mars.terraformingmars.util.XmlUtils;
import hr.terraforming.mars.terraformingmars.view.ScreenNavigator;
import hr.terraforming.mars.terraformingmars.controller.TerraformingMarsController;
import lombok.extern.slf4j.Slf4j;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class GameFlowManager {

    private final TerraformingMarsController controller;
    private GameManager gameManager;
    private GameBoard gameBoard;

    public GameFlowManager(TerraformingMarsController controller, GameManager gameManager, GameBoard gameBoard) {
        this.controller = controller;
        this.gameManager = gameManager;
        this.gameBoard = gameBoard;
    }

    public void updateState(GameManager newManager, GameBoard newBoard) {
        this.gameManager = newManager;
        this.gameBoard = newBoard;
        log.info("GameFlowManager updated with new GameManager/GameBoard.");
    }

    public void handleEndOfActionPhase() {
        log.info("All players have passed. Starting Production Phase.");

        gameManager.doProduction();

        controller.updateAllUI();

        if (gameBoard.isFinalGeneration()) {
            log.info("This was the last generation. Starting final greenery conversion phase.");

            ScreenNavigator.startFinalGreeneryPhase(
                    gameManager,
                    controller
            );
        } else {
            startNewGeneration();
        }
    }

    private void startNewGeneration() {
        log.info("Production phase is over. Starting a new generation.");

        gameManager.startNewGeneration();
        gameManager.resetDraftPhase();

        controller.setViewedPlayer(gameManager.getCurrentPlayer());
        controller.updateAllUI();

        ResearchPhaseManager researchManager = new ResearchPhaseManager(
                gameManager,
                controller.getSceneWindow(),
                controller,
                this::onResearchComplete
        );
        researchManager.start();
    }

    public void onResearchComplete() {
        log.info("🔬 onResearchComplete() called | currentPlayerIndex={} | currentPhase={} | Thread: {}",
                gameManager.getCurrentPlayer().getName(),
                gameManager.getCurrentPhase(),
                Thread.currentThread().getName());

        if (gameManager.getCurrentPhase() == GamePhase.ACTIONS) {
            log.warn("⚠️ Already in ACTIONS phase, skipping duplicate beginActionPhase()");
            return;
        }

        createAndSaveResearchPhaseSnapshot(gameManager);

        log.info("Research phase complete. Starting Action Phase.");

        log.info("✅ About to call beginActionPhase() | currentPlayerIndex BEFORE={}",
                gameManager.getCurrentPlayer().getName());

        gameManager.beginActionPhase();

        log.info("✅ After beginActionPhase() | currentPlayerIndex AFTER={}",
                gameManager.getCurrentPlayer().getName());

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