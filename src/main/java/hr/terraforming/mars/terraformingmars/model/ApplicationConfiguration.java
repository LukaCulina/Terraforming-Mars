package hr.terraforming.mars.terraformingmars.model;

import hr.terraforming.mars.terraformingmars.controller.TerraformingMarsController;
import hr.terraforming.mars.terraformingmars.enums.PlayerType;
import hr.terraforming.mars.terraformingmars.network.GameClientThread;
import hr.terraforming.mars.terraformingmars.network.GameServerThread;
import lombok.Getter;
import lombok.Setter;

public class ApplicationConfiguration {

    private static ApplicationConfiguration instance;
    @Getter @Setter
    private PlayerType playerType;
    @Getter @Setter
    private int playerCount;
    @Getter @Setter
    private String myPlayerName;
    @Getter @Setter
    private GameServerThread gameServer;
    @Getter @Setter
    private GameClientThread gameClient;

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
    private TerraformingMarsController activeGameController;

    public void setActiveGameController(TerraformingMarsController controller) {
        this.activeGameController = controller;
    }

    public TerraformingMarsController getActiveGameController() {
        return activeGameController;
    }

}