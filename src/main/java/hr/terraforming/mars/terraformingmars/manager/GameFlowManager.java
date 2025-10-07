package hr.terraforming.mars.terraformingmars.manager;

import hr.terraforming.mars.terraformingmars.view.GameScreens;
import hr.terraforming.mars.terraformingmars.controller.TerraformingMarsController;
import hr.terraforming.mars.terraformingmars.model.GameBoard;
import hr.terraforming.mars.terraformingmars.model.GameManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
                this::onResearchComplete
        );
        researchManager.start();
    }

    private void onResearchComplete() {
        logger.info("Research phase complete. Starting Action Phase.");

        gameManager.beginActionPhase();
        controller.updateAllUI();
    }
}
