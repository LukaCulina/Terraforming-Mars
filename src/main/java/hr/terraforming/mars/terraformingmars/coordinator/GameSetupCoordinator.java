package hr.terraforming.mars.terraformingmars.coordinator;

import hr.terraforming.mars.terraformingmars.controller.game.TerraformingMarsController;
import hr.terraforming.mars.terraformingmars.enums.PlayerType;
import hr.terraforming.mars.terraformingmars.manager.*;
import hr.terraforming.mars.terraformingmars.model.*;
import hr.terraforming.mars.terraformingmars.network.GameServerThread;
import hr.terraforming.mars.terraformingmars.replay.ReplayManager;
import hr.terraforming.mars.terraformingmars.ui.ResizeHandler;
import hr.terraforming.mars.terraformingmars.ui.UIInitializer;
import hr.terraforming.mars.terraformingmars.model.ApplicationConfiguration;
import javafx.application.Platform;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public record GameSetupCoordinator(TerraformingMarsController controller) {

    public void setupNewGame(GameState gameState) {
        setupGameInternal(gameState, true);
    }

    public void setupLoadedGame(GameState gameState) {
        setupGameInternal(gameState, false);
    }


    private void setupGameInternal(GameState gameState, boolean shouldStartGame) {
        controller.setGameManager(gameState.gameManager());
        controller.setGameBoard(gameState.gameBoard());

        if (controller.getGameBoard() == null) {
            log.error("GameBoard is null - cannot initialize!");
            return;
        }

        if (controller.getGameManager() != null) {
            controller.getGameManager().relink(controller.getGameBoard());
        }

        initializeComponents();

        setupChatIfAvailable();

        controller.getNetworkCoordinator().setupClientListeners();

        setupInitialPlayerView();

        if (shouldStartGame) {
            startGameIfLocalOrHost();
        }

        finalizeUISetup();
    }

    private void initializeComponents() {
        GameFlowManager gameFlowManager = new GameFlowManager(
                controller,
                controller.getGameManager(),
                controller.getGameBoard()
        );

        ActionManager actionManager = new ActionManager(
                controller,
                gameFlowManager
        );
        controller.setActionManager(actionManager);

        injectActionManagerToServer(actionManager);

        PlacementManager placementManager = new PlacementManager(
                controller,
                controller.getGameManager(),
                controller.getGameBoard(),
                actionManager
        );
        controller.setPlacementManager(placementManager);

        controller.setPlacementCoordinator(new PlacementCoordinator(placementManager));

        controller.setChatManager(new ChatManager(
                controller.getChatListView(),
                controller.getChatInput(),
                controller.getChatBoxContainer()
        ));

        controller.getCancelPlacementButton().setOnAction(
                _ -> placementManager.cancelPlacement()
        );

        UIManager uiManager = UIInitializer.initUI(
                controller,
                actionManager
        );
        controller.setUiManager(uiManager);

        ResizeHandler.attachResizeListeners(
                controller.getHexBoardPane(),
                uiManager.getHexBoardDrawer()
        );

        controller.getGameBoard().setOnGlobalParametersChanged(controller::updateAllUI);

        controller.setReplayManager(new ReplayManager(controller));
    }

    private void injectActionManagerToServer(ActionManager actionManager) {
        PlayerType playerType = ApplicationConfiguration.getInstance().getPlayerType();
        if (playerType == PlayerType.HOST) {
            GameServerThread server = ApplicationConfiguration.getInstance().getGameServer();
            if (server != null) {
                server.setActionManager(actionManager);
            } else {
                log.warn("GameServerThread is null, cannot inject ActionManager!");
            }
        }
    }

    private void setupChatIfAvailable() {
        PlayerType playerType = ApplicationConfiguration.getInstance().getPlayerType();
        ChatManager chatManager = controller.getChatManager();

        if (chatManager != null) {
            try {
                chatManager.setupChatSystem(playerType);
            } catch (Exception e) {
                log.warn("Chat unavailable, continuing without chat", e);
            }
        }
    }

    private void setupInitialPlayerView() {
        String myPlayerName = ApplicationConfiguration.getInstance().getMyPlayerName();
        Player playerToShow = controller.getGameManager().getCurrentPlayer();

        if (myPlayerName != null) {
            Player myPlayer = controller.getGameManager().getPlayerByName(myPlayerName);
            if (myPlayer != null) {
                playerToShow = myPlayer;
            }
        }
        controller.showPlayerBoard(playerToShow);
    }

    private void startGameIfLocalOrHost() {
        PlayerType playerType = ApplicationConfiguration.getInstance().getPlayerType();
        if (playerType == PlayerType.LOCAL || playerType == PlayerType.HOST) {
            controller.getGameManager().startGame();
        }
    }

    private void finalizeUISetup() {
        Platform.runLater(() -> controller.getGameBoardPane().maxHeightProperty().bind(
                controller.getHexBoardPane().getScene().heightProperty().subtract(28)
        ));

        Platform.runLater(controller::updateAllUI);
        controller.startMoveHistory();
        controller.updatePlayerHighlightForCurrentPlayer();
    }
}
