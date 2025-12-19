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
        for (Player p : players) {
            int income = p.getTR() + p.productionProperty(ResourceType.MEGA_CREDITS).get();
            p.addMC(income);

            int energy = p.resourceProperty(ResourceType.ENERGY).get();
            p.addResource(ResourceType.HEAT, energy);
            p.resourceProperty(ResourceType.ENERGY).set(0);

            p.getProductionMap().forEach((type, amount) -> {
                if (type != ResourceType.MEGA_CREDITS) {
                    p.addResource(type, amount.get());
                }
            });

            log.debug("{}: Produced {} MC, {} resources",
                    p.getName(), income, p.getProductionMap().size());
        }
    }
}