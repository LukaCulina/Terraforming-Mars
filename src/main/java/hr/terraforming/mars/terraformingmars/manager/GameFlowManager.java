package hr.terraforming.mars.terraformingmars.manager;

import hr.terraforming.mars.terraformingmars.enums.ActionType;
import hr.terraforming.mars.terraformingmars.model.*;
import hr.terraforming.mars.terraformingmars.util.XmlUtils;
import hr.terraforming.mars.terraformingmars.view.GameScreens;
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

            GameScreens.startFinalGreeneryPhase(
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

        log.info("Research phase complete. Starting Action Phase.");

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
        log.debug("RESEARCH_PHASE_COMPLETE snapshot saved to XML.");
    }
}