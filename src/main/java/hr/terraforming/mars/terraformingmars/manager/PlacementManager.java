package hr.terraforming.mars.terraformingmars.manager;

import hr.terraforming.mars.terraformingmars.controller.TerraformingMarsController;
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

    private final TerraformingMarsController mainController;
    private final GameBoard gameBoard;
    private final GameManager gameManager;
    private final ActionManager actionManager;

    private PlacementMode placementMode = PlacementMode.NONE;

    private StandardProject projectToPlace = null;
    private Card cardToPlace = null;
    @Getter
    private TileType tileTypeToPlace = null;
    private Player finalGreeneryPlayer = null;
    private Runnable onPlacementCompleteCallback = null;

    public PlacementManager(TerraformingMarsController mainController, GameManager gameManager, GameBoard gameBoard,ActionManager actionManager) {
        this.mainController = mainController;
        this.gameManager = gameManager;
        this.gameBoard = gameBoard;
        this.actionManager = actionManager;
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

    private void enterPlacementMode(PlacementMode mode, TileType tileType, GameMove move,
                                    Card card, StandardProject project, Player player, Runnable callback) {

        this.placementMode = mode;
        this.tileTypeToPlace = tileType;
        this.moveInProgress = move;
        this.cardToPlace = card;
        this.projectToPlace = project;
        this.finalGreeneryPlayer = player;
        this.onPlacementCompleteCallback = callback;

        log.info("Entering placement mode: {}", mode);
        mainController.drawBoard();
        mainController.setGameControlsEnabled(false);
        mainController.setCancelButtonVisible(true);
    }

    public void executePlacement(Tile selectedTile) {
        Player placementOwner = getPlacementOwner();
        String myName = ApplicationConfiguration.getInstance().getMyPlayerName();
        boolean isLocalGame = ApplicationConfiguration.getInstance().getPlayerType() == PlayerType.LOCAL;

        if (placementOwner == null) {
            log.error("Placement failed: No player defined for tile placement. This should not happen.");

            cancelPlacement();
            return;
        }

        if (!isLocalGame && !placementOwner.getName().equals(myName)) {
            log.error("🚫 Unauthorized placement attempt! Owner: {}, MyName: {}",
                    placementOwner.getName(), myName);
            cancelPlacement();
            return;
        }

        recordMoves(selectedTile, placementOwner);

        PlacementService placementService = new PlacementService(gameBoard);
        PlacementService.PlacementContext context = new PlacementService.PlacementContext(
                placementMode, selectedTile, placementOwner, gameManager,
                tileTypeToPlace, cardToPlace, projectToPlace
        );

        log.info("🔥 executePlacement() - placementMode = {}", placementMode);

        placementService.executeComplexPlacement(context);

        finishPlacement();

        log.info("🔥 AFTER finishPlacement() - calling performAction()? placementMode = {}", placementMode);

        if (placementMode != PlacementMode.FINAL_GREENERY) {
            log.info("🔥 CALLING performAction()");
            actionManager.performAction();
        } else {
            log.info("🔥 CALLING updateAllUI() for FINAL_GREENERY");
            mainController.updateAllUI();
        }
    }

    private void recordMoves(Tile tile, Player owner) {
        if (moveInProgress != null) {
            actionManager.recordAndSaveMove(moveInProgress);
        }

        GameMove placeTileMove = new GameMove(
                owner.getName(),
                ActionType.PLACE_TILE,
                "Placed " + tileTypeToPlace.name(),
                tile.getRow(),
                tile.getCol(),
                tileTypeToPlace,
                LocalDateTime.now()
        );

        actionManager.recordAndSaveMove(placeTileMove);
    }

    public void cancelPlacement() {
        log.info("Placement canceled by user.");

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
        mainController.setGameControlsEnabled(true);

        mainController.drawBoard();

        var config = ApplicationConfiguration.getInstance();
        NetworkBroadcaster broadcaster = config.getBroadcaster();
        if (broadcaster != null) {
            broadcaster.broadcast();
            log.info("HOST broadcasted GameState AFTER placement");
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
                log.info("Placement owner from move: {}", movePlayer.getName());
                return movePlayer;
            }
        }

        if (placementMode == PlacementMode.FINAL_GREENERY) {
            return finalGreeneryPlayer;
        }

        return gameManager.getCurrentPlayer();
    }
}