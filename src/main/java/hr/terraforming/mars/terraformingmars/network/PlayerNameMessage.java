package hr.terraforming.mars.terraformingmars.network;

import java.io.Serializable;

public record PlayerNameMessage(String playerName) implements Serializable {
}
