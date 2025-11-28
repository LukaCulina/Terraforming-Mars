package hr.terraforming.mars.terraformingmars.network;

import hr.terraforming.mars.terraformingmars.enums.PlayerType;
import hr.terraforming.mars.terraformingmars.model.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public record NetworkBroadcaster(GameManager gameManager, GameBoard gameBoard) {

    public void broadcast() {
        PlayerType playerType = ApplicationConfiguration.getInstance().getPlayerType();

        if (playerType == PlayerType.HOST) {
            GameServerThread server = ApplicationConfiguration.getInstance().getGameServer();
            if (server != null) {
                server.broadcastGameState(new GameState(gameManager, gameBoard));
                log.debug("📡 Broadcast sent");
            }
        }
    }
}
