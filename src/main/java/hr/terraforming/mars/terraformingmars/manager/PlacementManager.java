package hr.terraforming.mars.terraformingmars.manager;

import hr.terraforming.mars.terraformingmars.controller.game.GameScreenController;
import hr.terraforming.mars.terraformingmars.coordinator.FinalGreeneryCoordinator;
import hr.terraforming.mars.terraformingmars.enums.*;
import hr.terraforming.mars.terraformingmars.model.*;
import hr.terraforming.mars.terraformingmars.network.NetworkBroadcaster;
import hr.terraforming.mars.terraformingmars.service.PlacementService;
import javafx.application.Platform;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import java.time.LocalDateTime;

@Slf4j
public class PlacementManager {
    private GameMove moveInProgress;

    private final GameScreenController mainController;
    private final GameBoard gameBoard;
    private final GameManager gameManager;
    private final ActionManager actionManager;
    private final FinalGreeneryCoordinator finalGreeneryCoordinator;

    private PlacementMode placementMode = PlacementMode.NONE;

    private StandardProject projectToPlace = null;
    private Card cardToPlace = null;
    @Getter
    private TileType tileTypeToPlace = null;
    private Player finalGreeneryPlayer = null;
    private Runnable onPlacementCompleteCallback = null;

    public PlacementManager(GameScreenController mainController, GameManager gameManager, GameBoard gameBoard, ActionManager actionManager) {
        this.mainController = mainController;
        this.gameManager = gameManager;
        this.gameBoard = gameBoard;
        this.actionManager = actionManager;
        this.finalGreeneryCoordinator = new FinalGreeneryCoordinator(this);
    }

    public boolean isPlacementMode() {
        return placementMode != PlacementMode.NONE;
    }

    public void enterPlacementModeForPlant(GameMove move) {
        enterPlacementMode(PlacementMode.PLANT_CONVERSION, TileType.GREENERY, move, null, null, null, null);
    }

    public void enterPlacementModeForProject(StandardProject project, GameMove move) {
        enterPlacementMode(PlacementMode.STANDARD_PROJECT, project.getTileType(), move, null, project, null, null);
    }

    public void enterPlacementModeForCard(Card card, GameMove move) {
        enterPlacementMode(PlacementMode.CARD, card.getTileToPlace(), move, card, null, null, null);
    }

    public void enterPlacementModeForFinalGreenery(Player player, Runnable callback) {
        enterPlacementMode(PlacementMode.FINAL_GREENERY, TileType.GREENERY, null, null, null, player, callback);
    }

    public void startFinalGreeneryPlacement(Player player, Runnable onComplete) {
        mainController.setGameControlsEnabled(false);
        mainController.setCancelButtonVisible(false);
        finalGreeneryCoordinator.startFinalGreeneryPlacement(player, onComplete);
    }

    private void enterPlacementMode(PlacementMode mode, TileType tileType, GameMove move,
                                    Card card, StandardProject project, Player player, Runnable callback) {

        this.placementMode = mode;
        this.tileTypeToPlace = tileType;
        this.moveInProgress = move;
        this.cardToPlace = card;
        this.projectToPlace = project;
        this.finalGreeneryPlayer = player;
        this.onPlacementCompleteCallback = callback;

        mainController.drawBoard();
        if (mode != PlacementMode.FINAL_GREENERY) {
            mainController.setGameControlsEnabled(false);
            mainController.setCancelButtonVisible(true);
        }
    }

    public void executePlacement(Tile selectedTile) {
        Player placementOwner = getPlacementOwner();
        String myName = ApplicationConfiguration.getInstance().getMyPlayerName();
        boolean isLocalGame = ApplicationConfiguration.getInstance().getPlayerType() == PlayerType.LOCAL;

        if (placementOwner == null || (!isLocalGame && !placementOwner.getName().equals(myName))) {
            cancelPlacement();
            return;
        }

        recordMoves(selectedTile, placementOwner);

        PlacementService placementService = new PlacementService(gameBoard);
        PlacementService.PlacementContext context = new PlacementService.PlacementContext(
                placementMode, selectedTile, placementOwner, gameManager,
                tileTypeToPlace, cardToPlace, projectToPlace
        );

        placementService.executeComplexPlacement(context);

        finishPlacement();
    }

    private void recordMoves(Tile tile, Player owner) {
        if (moveInProgress != null) {
            actionManager.recordAndSaveMove(moveInProgress);
        }

        GameMove placeTileMove = new GameMove(
                owner.getName(),
                ActionType.PLACE_TILE,
                "placed " + tileTypeToPlace.name(),
                tile.getRow(),
                tile.getCol(),
                tileTypeToPlace,
                LocalDateTime.now()
        );

        actionManager.recordAndSaveMove(placeTileMove);
    }

    public void cancelPlacement() {
        if (finalGreeneryCoordinator.isActive()) {
            finalGreeneryCoordinator.cancel();
        }

        if (onPlacementCompleteCallback != null) {
            Platform.runLater(onPlacementCompleteCallback);
        }

        resetAllState();
        mainController.setGameControlsEnabled(true);
        mainController.drawBoard();
    }

    private void finishPlacement() {
        boolean wasFinalGreenery = (placementMode == PlacementMode.FINAL_GREENERY);

        resetPlacementState();
        if (!wasFinalGreenery) {
            mainController.setGameControlsEnabled(true);
        }

        mainController.drawBoard();

        var config = ApplicationConfiguration.getInstance();
        NetworkBroadcaster broadcaster = config.getBroadcaster();
        if (broadcaster != null) {
            broadcaster.broadcast();
        }

        if (!wasFinalGreenery) {
            actionManager.performAction();
        }

        if (wasFinalGreenery && onPlacementCompleteCallback != null) {
            Platform.runLater(onPlacementCompleteCallback);
        }
    }

    private void resetPlacementState() {
        this.placementMode = PlacementMode.NONE;
        this.projectToPlace = null;
        this.cardToPlace = null;
        this.tileTypeToPlace = null;
        this.moveInProgress = null;
        mainController.setCancelButtonVisible(false);
    }

    private void resetAllState() {
        resetPlacementState();
        this.finalGreeneryPlayer = null;
        this.onPlacementCompleteCallback = null;
    }

    public Player getPlacementOwner() {
        if (moveInProgress != null && moveInProgress.playerName() != null) {
            Player movePlayer = gameManager.getPlayerByName(moveInProgress.playerName());
            if (movePlayer != null) {
                return movePlayer;
            }
        }

        if (placementMode == PlacementMode.FINAL_GREENERY) {
            return finalGreeneryPlayer;
        }

        return gameManager.getCurrentPlayer();
    }
}