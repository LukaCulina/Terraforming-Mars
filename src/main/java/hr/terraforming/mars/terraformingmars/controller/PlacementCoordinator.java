package hr.terraforming.mars.terraformingmars.controller;

import hr.terraforming.mars.terraformingmars.enums.StandardProject;
import hr.terraforming.mars.terraformingmars.manager.PlacementManager;
import hr.terraforming.mars.terraformingmars.model.Card;
import hr.terraforming.mars.terraformingmars.model.GameMove;
import hr.terraforming.mars.terraformingmars.model.Player;

public record PlacementCoordinator(PlacementManager placementManager) {

    public void enterPlacementModeForCard(Card card, GameMove move) {
        placementManager.enterPlacementModeForCard(card, move);
    }

    public void enterPlacementModeForProject(StandardProject project, GameMove move) {
        placementManager.enterPlacementModeForProject(project, move);
    }

    public void enterPlacementModeForPlant(GameMove move) {
        placementManager.enterPlacementModeForPlant(move);
    }

    public void enterPlacementModeForFinalGreenery(Player player, Runnable onCompleteCallback) {
        placementManager.enterPlacementModeForFinalGreenery(player, onCompleteCallback);
    }
}
