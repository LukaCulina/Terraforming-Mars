package hr.terraforming.mars.terraformingmars.coordinator;

import hr.terraforming.mars.terraformingmars.enums.ResourceType;
import hr.terraforming.mars.terraformingmars.manager.PlacementManager;
import hr.terraforming.mars.terraformingmars.model.Player;
import javafx.application.Platform;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FinalGreeneryCoordinator {

    private final PlacementManager placementManager;

    private Player currentPlayer;
    private int remainingGreeneryCount = 0;
    private Runnable onComplete;

    public FinalGreeneryCoordinator(PlacementManager placementManager) {
        this.placementManager = placementManager;
    }

    public void startFinalGreeneryPlacement(Player player, Runnable onComplete) {
        int plants = player.resourceProperty(ResourceType.PLANTS).get();
        int cost = player.getGreeneryCost();
        int greeneryCount = plants / cost;

        log.info("Starting Final Greenery placement for {}", player.getName());

        currentPlayer = player;
        remainingGreeneryCount = greeneryCount;
        this.onComplete = onComplete;

        if (greeneryCount == 0) {
            log.info("Player {} has no greenery to place", player.getName());
            finish();
            return;
        }

        placeNextGreenery();
    }


    private void placeNextGreenery() {
        if (remainingGreeneryCount <= 0) {
            finish();
            return;
        }

        placementManager.enterPlacementModeForFinalGreenery(
                currentPlayer,
                () -> {
                    remainingGreeneryCount--;
                    Platform.runLater(this::placeNextGreenery);
                }
        );
    }

    private void finish() {
        log.info("Final Greenery placement completed for {}",
                currentPlayer != null ? currentPlayer.getName() : "unknown");

        Runnable callback = onComplete;

        currentPlayer = null;
        remainingGreeneryCount = 0;
        onComplete = null;

        if (callback != null) {
            Platform.runLater(callback);
        }
    }


    public void cancel() {
        if (isActive()) {
            log.info("Final Greenery placement cancelled for {} ({} remaining)",
                    currentPlayer.getName(), remainingGreeneryCount);
            finish();
        }
    }

    public boolean isActive() {
        return remainingGreeneryCount > 0 && currentPlayer != null;
    }
}
