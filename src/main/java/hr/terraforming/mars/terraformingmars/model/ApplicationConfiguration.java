package hr.terraforming.mars.terraformingmars.model;

import hr.terraforming.mars.terraformingmars.controller.game.GameScreenController;
import hr.terraforming.mars.terraformingmars.enums.PlayerType;
import hr.terraforming.mars.terraformingmars.network.GameClientThread;
import hr.terraforming.mars.terraformingmars.network.GameServerThread;
import hr.terraforming.mars.terraformingmars.network.NetworkBroadcaster;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ApplicationConfiguration {

    private static ApplicationConfiguration instance;
    private PlayerType playerType;
    private int playerCount;
    private String myPlayerName;

    private GameServerThread gameServer;
    private GameClientThread gameClient;
    private NetworkBroadcaster broadcaster;

    private GameScreenController activeGameController;

    private ApplicationConfiguration() {
        this.playerType = PlayerType.LOCAL;
        this.playerCount = 2;
    }

    public static ApplicationConfiguration getInstance() {
        if (instance == null) {
            instance = new ApplicationConfiguration();
        }
        return instance;
    }
}