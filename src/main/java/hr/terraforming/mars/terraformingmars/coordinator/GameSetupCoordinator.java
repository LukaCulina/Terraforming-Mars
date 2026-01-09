package hr.terraforming.mars.terraformingmars.coordinator;

import hr.terraforming.mars.terraformingmars.controller.game.GameScreenController;
import hr.terraforming.mars.terraformingmars.enums.PlayerType;
import hr.terraforming.mars.terraformingmars.manager.*;
import hr.terraforming.mars.terraformingmars.model.*;
import hr.terraforming.mars.terraformingmars.network.GameServerThread;
import hr.terraforming.mars.terraformingmars.replay.ReplayManager;
import hr.terraforming.mars.terraformingmars.ui.GameScreenResizer;
import hr.terraforming.mars.terraformingmars.ui.GameScreenInitializer;
import hr.terraforming.mars.terraformingmars.model.ApplicationConfiguration;
import javafx.application.Platform;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public record GameSetupCoordinator(GameScreenController controller) {

    public void setupNewGame(GameState gameState) {
        setupGame(gameState, true);
    }

    public void setupLoadedGame(GameState gameState) {
        setupGame(gameState, false);
    }

    private void setupGame(GameState gameState, boolean isNewGame) {
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

        setupChat();

        controller.getNetworkCoordinator().setupClientListeners();

        if (isNewGame) {
            startGame();
        }

        setupInitialPlayer();

        finalizeUISetup();
    }

    private void initializeComponents() {
        GameFlowManager gameFlowManager = new GameFlowManager(
                controller
        );

        ActionManager actionManager = new ActionManager(
                controller,
                gameFlowManager
        );
        controller.setActionManager(actionManager);

        setupActionManager(actionManager);

        PlacementManager placementManager = new PlacementManager(
                controller,
                controller.getGameManager(),
                controller.getGameBoard(),
                actionManager
        );
        controller.setPlacementManager(placementManager);

        controller.setChatManager(new ChatManager(
                controller.getChatListView(),
                controller.getChatInput(),
                controller.getChatBoxContainer()
        ));

        controller.getCancelPlacementButton().setOnAction(
                _ -> placementManager.cancelPlacement()
        );

        GameScreenManager gameScreenManager = GameScreenInitializer.initializeUI(
                controller,
                actionManager
        );
        controller.setGameScreenManager(gameScreenManager);

        GameScreenResizer.attachResizeListeners(
                controller.getHexBoardPane(),
                gameScreenManager.getHexBoardDrawer()
        );

        controller.getGameBoard().setOnGlobalParametersChanged(controller::refreshGameScreen);

        controller.setReplayManager(new ReplayManager(controller));
    }

    private void setupActionManager(ActionManager actionManager) {
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

    private void setupChat() {
        PlayerType playerType = ApplicationConfiguration.getInstance().getPlayerType();
        ChatManager chatManager = controller.getChatManager();

        if (chatManager != null) {
            try {
                chatManager.setupChatSystem(playerType);

                if (playerType == PlayerType.HOST) {
                    chatManager.clearHistory();
                    log.info("Chat history cleared for new game");
                }
            } catch (Exception e) {
                log.warn("Chat unavailable, continuing without chat", e);
            }
        }
    }

    private void setupInitialPlayer() {
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

    private void startGame() {
        PlayerType playerType = ApplicationConfiguration.getInstance().getPlayerType();

        if (playerType == PlayerType.LOCAL || playerType == PlayerType.HOST) {
            controller.getGameManager().startGame();
        }
    }

    private void finalizeUISetup() {
        Platform.runLater(() -> controller.getGameBoardPane().maxHeightProperty().bind(
                controller.getHexBoardPane().getScene().heightProperty().subtract(28)
        ));

        Platform.runLater(controller::refreshGameScreen);
        controller.startMoveHistory();
        controller.updatePlayerHighlightForCurrentPlayer();
    }
}