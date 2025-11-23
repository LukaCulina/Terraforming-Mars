package hr.terraforming.mars.terraformingmars.network;

import hr.terraforming.mars.terraformingmars.model.GameState;

@FunctionalInterface
public interface GameStateListener {
    void onGameStateReceived(GameState state);
}
