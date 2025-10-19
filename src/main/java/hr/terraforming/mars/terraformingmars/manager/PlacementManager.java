package hr.terraforming.mars.terraformingmars.manager;

import hr.terraforming.mars.terraformingmars.controller.TerraformingMarsController;
import hr.terraforming.mars.terraformingmars.enums.ActionType;
import hr.terraforming.mars.terraformingmars.enums.ResourceType;
import hr.terraforming.mars.terraformingmars.enums.StandardProject;
import hr.terraforming.mars.terraformingmars.enums.TileType;
import hr.terraforming.mars.terraformingmars.model.*;
import hr.terraforming.mars.terraformingmars.service.CostService;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;

public class PlacementManager {
    private GameMove moveInProgress;
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

    public void enterPlacementModeForPlant(GameMove move) {
        this.tileTypeToPlace = TileType.GREENERY;
        this.isPlantConversionMode = true;
        this.isPlacementMode = true;
        this.moveInProgress = move;
        logger.info("Entering placement mode for Plant Conversion.");
        mainController.drawBoard();
        mainController.setGameControlsEnabled(false);
    }

    public void enterPlacementModeForProject(StandardProject project, GameMove move) {
        this.projectToPlace = project;
        this.tileTypeToPlace = project.getTileType();
        this.moveInProgress = move;
        this.isPlacementMode = true;
        logger.info("Entering placement mode for project: {}", project.getName());
        mainController.drawBoard();
        mainController.setGameControlsEnabled(false);

    }

    public void enterPlacementModeForCard(Card card, GameMove move) {
        this.cardToPlace = card;
        this.tileTypeToPlace = card.getTileToPlace();
        this.moveInProgress = move;
        this.isPlacementMode = true;

        logger.info("Entering placement mode for card: {}", card.getName());
        mainController.drawBoard();
        mainController.setGameControlsEnabled(false);
    }

    public void enterPlacementModeForFinalGreenery(Player player, Runnable onCompleteCallback) {
        this.isFinalGreeneryMode = true;
        this.isPlacementMode = true;
        this.tileTypeToPlace = TileType.GREENERY;
        this.finalGreeneryPlayer = player;
        this.onPlacementCompleteCallback = onCompleteCallback;

        logger.info("Entering placement mode for FINAL GREENERY for player: {}", player.getName());
        mainController.drawBoard();
        mainController.setGameControlsEnabled(false);
    }

    public void executePlacement(Tile selectedTile) {
        Player currentPlayer = gameManager.getCurrentPlayer();

        Player placementOwner = isFinalGreeneryMode ? this.finalGreeneryPlayer : currentPlayer;
        if (placementOwner == null) {
            logger.error("Placement failed: No player defined for tile placement. This should not happen.");

            cancelPlacement();
            return;
        }
        if (moveInProgress != null) {
            actionManager.recordAndSaveMove(moveInProgress);
        }

        GameMove placeTileMove = new GameMove(
                placementOwner.getName(),
                ActionType.PLACE_TILE,
                "Placed " + tileTypeToPlace.name(),
                LocalDateTime.now()
        );

        placeTileMove.setRow(selectedTile.getRow());
        placeTileMove.setCol(selectedTile.getCol());
        placeTileMove.setTileType(tileTypeToPlace);
        actionManager.recordAndSaveMove(placeTileMove);
        if (isFinalGreeneryMode) {
            handleFinalGreeneryPlacement(selectedTile, placementOwner);
        }
        else if (isPlantConversionMode) {
            gameBoard.placeGreenery(selectedTile, placementOwner);
            placementOwner.spendPlantsForGreenery();
        }
        else {
            handleStandardPlacement(selectedTile, placementOwner);
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
        mainController.setGameControlsEnabled(true);
        mainController.drawBoard();
    }

    private void finishPlacement() {
        boolean wasFinalGreenery = this.isFinalGreeneryMode;

        resetPlacementState();
        mainController.setGameControlsEnabled(true);
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

    private void handleStandardPlacement(Tile tile, Player owner) {
        switch (tileTypeToPlace) {
            case OCEAN: gameBoard.placeOcean(tile, owner); break;
            case GREENERY: gameBoard.placeGreenery(tile, owner); break;
            case CITY: gameBoard.placeCity(tile, owner); break;
            default: logger.error("Trying to place an unexpected tile type: {}.", tileTypeToPlace); return;

        }
        if (cardToPlace != null) {
            owner.playCard(cardToPlace, gameManager);
        } else if (projectToPlace != null) {
            int finalCost = CostService.getFinalProjectCost(projectToPlace, owner);
            owner.spendMC(finalCost);
        }
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