package hr.terraforming.mars.terraformingmars.model;

import hr.terraforming.mars.terraformingmars.enums.PlayerType;
import lombok.Getter;
import lombok.Setter;

public class ApplicationConfiguration {

    private static ApplicationConfiguration instance;
    @Getter
    @Setter
    private PlayerType playerType;
    @Getter
    @Setter
    private int playerCount;
    @Getter
    @Setter
    private String myPlayerName;

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