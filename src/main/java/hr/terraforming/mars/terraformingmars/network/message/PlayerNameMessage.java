package hr.terraforming.mars.terraformingmars.network.message;

import java.io.Serializable;

public record PlayerNameMessage(String playerName) implements Serializable {
}
