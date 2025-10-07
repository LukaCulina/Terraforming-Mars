package hr.terraforming.mars.terraformingmars.manager;

import hr.terraforming.mars.terraformingmars.controller.TerraformingMarsController;
import hr.terraforming.mars.terraformingmars.enums.ResourceType;
import hr.terraforming.mars.terraformingmars.enums.StandardProject;
import hr.terraforming.mars.terraformingmars.enums.TileType;
import hr.terraforming.mars.terraformingmars.model.*;
import hr.terraforming.mars.terraformingmars.service.CostService;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PlacementManager {

    private static final Logger logger = LoggerFactory.getLogger(PlacementManager.class);

    private final TerraformingMarsController mainController;
    private final GameBoard gameBoard;
    private final GameManager gameManager;
    private ActionManager actionManager;

    private boolean isPlacementMode = false;
    private boolean isFinalGreeneryMode = false;
    private boolean isPlantConversionMode = false;
    private Player finalGreeneryPlayer = null;
    private Runnable onPlacementCompleteCallback = null;

    private StandardProject projectToPlace = null;
    private Card cardToPlace = null;
    private TileType tileTypeToPlace = null;

    public PlacementManager(TerraformingMarsController mainController, GameManager gameManager, GameBoard gameBoard) {
        this.mainController = mainController;
        this.gameManager = gameManager;
        this.gameBoard = gameBoard;
    }

    public void setActionHandler(ActionManager actionManager) {
        this.actionManager = actionManager;
    }

    public void enterPlacementModeForPlant() {
        this.tileTypeToPlace = TileType.GREENERY;
        this.isPlantConversionMode = true;
        this.isPlacementMode = true;
        logger.info("Entering placement mode for Plant Conversion.");
        mainController.setPlacementUIVisible(true);
        mainController.drawBoard();
    }

    public void enterPlacementModeForProject(StandardProject project) {
        this.projectToPlace = project;
        this.tileTypeToPlace = project.getTileType();
        this.isPlacementMode = true;
        logger.info("Entering placement mode for project: {}", project.getName());
        mainController.setPlacementUIVisible(true);
        mainController.drawBoard();
    }

    public void enterPlacementModeForCard(Card card) {
        this.cardToPlace = card;
        this.tileTypeToPlace = card.getTileToPlace();
        this.isPlacementMode = true;

        logger.info("Entering placement mode for card: {}", card.getName());
        mainController.setPlacementUIVisible(true);
        mainController.drawBoard();
    }

    public void enterPlacementModeForFinalGreenery(Player player, Runnable onCompleteCallback) {
        this.isFinalGreeneryMode = true;
        this.isPlacementMode = true;
        this.tileTypeToPlace = TileType.GREENERY;
        this.finalGreeneryPlayer = player;
        this.onPlacementCompleteCallback = onCompleteCallback;

        logger.info("Entering placement mode for FINAL GREENERY for player: {}", player.getName());
        mainController.setPlacementUIVisible(true);
        mainController.drawBoard();
    }

    public void executePlacement(Tile selectedTile) {
        Player currentPlayer = gameManager.getCurrentPlayer();

        Player placementOwner = isFinalGreeneryMode ? this.finalGreeneryPlayer : currentPlayer;
        if (placementOwner == null) {
            logger.error("Placement failed: No player defined for tile placement. This should not happen.");

            cancelPlacement();
            return;
        }

        if (isFinalGreeneryMode) {
            handleFinalGreeneryPlacement(selectedTile, placementOwner);
            actionManager.recordTileMove("Placed final greenery tile", selectedTile);
        }
        else if (isPlantConversionMode) {
            gameBoard.placeGreenery(selectedTile, placementOwner);
            placementOwner.spendPlantsForGreenery();
            actionManager.recordTileMove("Converted plants to place a Greenery tile", selectedTile);
        }
        else {
            String moveDescription = handleStandardPlacement(selectedTile, placementOwner);
            actionManager.recordTileMove(moveDescription, selectedTile);
        }

        finishPlacement();

        if (!isFinalGreeneryMode) {
            actionManager.performAction();
        } else {
            mainController.updateAllUI();
        }
    }

    public void cancelPlacement() {
        logger.info("Placement canceled by user.");

        if (onPlacementCompleteCallback != null) {
            Platform.runLater(onPlacementCompleteCallback);
        }

        resetAllState();
        mainController.setPlacementUIVisible(false);
        mainController.drawBoard();
    }

    private void finishPlacement() {
        boolean wasFinalGreenery = this.isFinalGreeneryMode;

        resetPlacementState();
        mainController.setPlacementUIVisible(false);
        mainController.drawBoard();

        if (wasFinalGreenery && onPlacementCompleteCallback != null) {
            Platform.runLater(onPlacementCompleteCallback);
        }
    }

    private void handleFinalGreeneryPlacement(Tile tile, Player owner) {
        int cost = owner.getGreeneryCost();
        if (owner.resourceProperty(ResourceType.PLANTS).get() >= cost) {
            owner.spendPlantsForGreenery();
            gameBoard.placeGreenery(tile, owner);
            logger.info("{} placed a final greenery tile.", owner.getName());
        } else {
            logger.warn("{} does not have enough plants for final greenery conversion (has {}, needs {}).",
                    owner.getName(), owner.resourceProperty(ResourceType.PLANTS).get(), cost);
        }
    }

    private String handleStandardPlacement(Tile tile, Player owner) {
        String baseAction;
        switch (tileTypeToPlace) {
            case OCEAN: gameBoard.placeOcean(tile, owner); baseAction = "Placed an Ocean tile"; break;
            case GREENERY: gameBoard.placeGreenery(tile, owner); baseAction = "Placed a Greenery tile"; break;
            case CITY: gameBoard.placeCity(tile, owner); baseAction = "Placed a City tile"; break;
            default: logger.error("Trying to place an unexpected tile type: {}.", tileTypeToPlace); return "Error: Tried to place unknown tile";

        }
        if (cardToPlace != null) {
            owner.playCard(cardToPlace, gameManager);
            return baseAction + " from card: " + cardToPlace.getName();
        } else if (projectToPlace != null) {
            int finalCost = CostService.getFinalProjectCost(projectToPlace, owner);
            owner.spendMC(finalCost);
            return baseAction + " via standard project: " + projectToPlace.getName();
        }
        return "Error: Placement source unknown";
    }

    private void resetPlacementState() {
        this.isPlacementMode = false;
        this.projectToPlace = null;
        this.cardToPlace = null;
        this.tileTypeToPlace = null;
        this.isPlantConversionMode = false;
    }

    private void resetAllState() {
        resetPlacementState();
        this.isFinalGreeneryMode = false;
        this.finalGreeneryPlayer = null;
        this.onPlacementCompleteCallback = null;
    }

    public boolean isPlacementMode() {
        return isPlacementMode;
    }

    public Player getPlacementOwner() {
        return isFinalGreeneryMode ? finalGreeneryPlayer : gameManager.getCurrentPlayer();
    }

    public TileType getTileTypeToPlace() {
        return tileTypeToPlace;
    }
}