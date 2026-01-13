package hr.terraforming.mars.terraformingmars.service;

import hr.terraforming.mars.terraformingmars.enums.StandardProject;
import hr.terraforming.mars.terraformingmars.model.Corporation;
import hr.terraforming.mars.terraformingmars.model.Player;

public final class CostService {

    private CostService() {
        throw new IllegalStateException("Utility class");
    }

    public static int getFinalProjectCost(StandardProject project, Player player) {
        Corporation corporation = player.getCorporation();

        int finalCost = project.getCost();

        if (corporation != null) {
            if (corporation.name().equals("Tharsis Republic") && project == StandardProject.CITY) {
                finalCost -= 4;
            } else if (corporation.name().equals("Thorgate") && project == StandardProject.POWER_PLANT) {
                finalCost -= 3;
            }
        }

        return Math.max(0, finalCost);
    }
}
