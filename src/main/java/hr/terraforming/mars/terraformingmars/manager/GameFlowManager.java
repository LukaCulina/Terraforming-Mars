package hr.terraforming.mars.terraformingmars.manager;

import hr.terraforming.mars.terraformingmars.enums.ActionType;
import hr.terraforming.mars.terraformingmars.model.*;
import hr.terraforming.mars.terraformingmars.util.XmlUtils;
import hr.terraforming.mars.terraformingmars.view.GameScreens;
import hr.terraforming.mars.terraformingmars.controller.TerraformingMarsController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public record GameFlowManager(TerraformingMarsController controller, GameManager gameManager, GameBoard gameBoard) {

    private static final Logger logger = LoggerFactory.getLogger(GameFlowManager.class);

    public void handleEndOfActionPhase() {
        logger.info("All players have passed. Starting Production Phase.");

        gameManager.doProduction();

        controller.updateAllUI();

        if (gameBoard.isFinalGeneration()) {
            logger.info("This was the last generation. Starting final greenery conversion phase.");

            GameScreens.startFinalGreeneryPhase(
                    gameManager.getPlayers(),
                    controller
            );
        } else {
            startNewGeneration();
        }
    }

    private void startNewGeneration() {
        logger.info("Production phase is over. Starting a new generation.");

        gameManager.startNewGeneration();
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

    private void onResearchComplete() {
        createAndSaveResearchPhaseSnapshot(gameManager);

        logger.info("Research phase complete. Starting Action Phase.");

        gameManager.beginActionPhase();
        controller.updateAllUI();
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
        logger.debug("RESEARCH_PHASE_COMPLETE snapshot saved to XML.");
    }
}
