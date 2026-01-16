package hr.terraforming.mars.terraformingmars.service;

import hr.terraforming.mars.terraformingmars.enums.ResourceType;
import hr.terraforming.mars.terraformingmars.model.Player;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class ProductionService {

    private ProductionService() {
        throw new IllegalStateException("Service class - use static methods");
    }

    public static void executeProduction(List<Player> players) {
        for (Player player : players) {
            int income = player.getTR() + player.productionProperty(ResourceType.MEGA_CREDITS).get();
            player.addMC(income);

            int energy = player.resourceProperty(ResourceType.ENERGY).get();
            player.addResource(ResourceType.HEAT, energy);
            player.resourceProperty(ResourceType.ENERGY).set(0);

            player.getProductionMap().forEach((type, amount) -> {
                if (type != ResourceType.MEGA_CREDITS) {
                    player.addResource(type, amount.get());
                }
            });

            log.debug("{}: Produced {} MC, {} resources",
                    player.getName(), income, player.getProductionMap().size());
        }
    }
}