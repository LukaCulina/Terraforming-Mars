package hr.terraforming.mars.terraformingmars.manager;

import hr.terraforming.mars.terraformingmars.model.ApplicationConfiguration;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class GameSessionManager {

    private GameSessionManager() {
        throw new IllegalStateException("Utility class");
    }

    public static void resetForNewGame() {
        log.info("Resetting application state for new game");

        ApplicationConfiguration config = ApplicationConfiguration.getInstance();

        var server = config.getGameServer();
        if (server != null) {
            server.shutdown();
            config.setGameServer(null);
        }

        var client = config.getGameClient();
        if (client != null) {
            client.shutdown();
            config.setGameClient(null);
        }

        config.setActiveGameController(null);
        config.setBroadcaster(null);
        config.setPlayerType(null);
        config.setMyPlayerName(null);
    }
}
