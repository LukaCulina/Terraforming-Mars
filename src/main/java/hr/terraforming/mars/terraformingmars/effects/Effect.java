package hr.terraforming.mars.terraformingmars.effects;

import hr.terraforming.mars.terraformingmars.model.Player;
import hr.terraforming.mars.terraformingmars.model.GameManager;

@FunctionalInterface
public interface Effect {
    void execute(Player player, GameManager gameManager);
}

