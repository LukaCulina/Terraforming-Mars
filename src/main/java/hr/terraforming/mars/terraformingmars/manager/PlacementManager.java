package hr.terraforming.mars.terraformingmars.manager;

import hr.terraforming.mars.terraformingmars.controller.TerraformingMarsController;
import hr.terraforming.mars.terraformingmars.enums.*;
import hr.terraforming.mars.terraformingmars.model.*;
import hr.terraforming.mars.terraformingmars.network.NetworkBroadcaster;
import hr.terraforming.mars.terraformingmars.service.CostService;
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

        performPlacement(selectedTile, placementOwner);

        finishPlacement();

        if (placementMode != PlacementMode.FINAL_GREENERY) {
            actionManager.performAction();
        } else {
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

    private void performPlacement(Tile tile, Player owner) {
        switch (placementMode) {
            case FINAL_GREENERY -> handleFinalGreeneryPlacement(tile, owner);
            case PLANT_CONVERSION -> handlePlantConversion(tile, owner);
            case STANDARD_PROJECT, CARD -> handleStandardPlacement(tile, owner);
            default -> log.error("Unknown placement mode: {}", placementMode);
        }
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

    private void handleFinalGreeneryPlacement(Tile tile, Player owner) {
        int cost = owner.getGreeneryCost();
        if (owner.resourceProperty(ResourceType.PLANTS).get() >= cost) {
            owner.spendPlantsForGreenery();
            gameBoard.placeGreenery(tile, owner);
            log.info("{} placed a final greenery tile.", owner.getName());
        } else {
            log.warn("{} does not have enough plants for final greenery conversion (has {}, needs {}).",
                    owner.getName(), owner.resourceProperty(ResourceType.PLANTS).get(), cost);
        }
    }

    private void handlePlantConversion(Tile tile, Player owner) {
        gameBoard.placeGreenery(tile, owner);
        owner.spendPlantsForGreenery();
    }

    private void handleStandardPlacement(Tile tile, Player owner) {
        switch (tileTypeToPlace) {
            case OCEAN: gameBoard.placeOcean(tile, owner); break;
            case GREENERY: gameBoard.placeGreenery(tile, owner); break;
            case CITY: gameBoard.placeCity(tile, owner); break;
            default: log.error("Trying to place an unexpected tile type: {}.", tileTypeToPlace); return;
        }
        if (cardToPlace != null) {
            owner.playCard(cardToPlace, gameManager);
        } else if (projectToPlace != null) {
            int finalCost = CostService.getFinalProjectCost(projectToPlace, owner);
            owner.spendMC(finalCost);
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