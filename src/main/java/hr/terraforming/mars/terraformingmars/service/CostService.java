package hr.terraforming.mars.terraformingmars.service;

import hr.terraforming.mars.terraformingmars.enums.StandardProject;
import hr.terraforming.mars.terraformingmars.model.Corporation;
import hr.terraforming.mars.terraformingmars.model.Player;


public final class CostService {

    private CostService() {
        throw new IllegalStateException("Utility class");
    }

    public static int getFinalProjectCost(StandardProject project, Player player) {
        Corporation corp = player.getCorporation();
        int finalCost = project.getCost();

        if (corp != null) {
            if (corp.name().equals("Tharsis Republic") && project == StandardProject.CITY) {
                finalCost -= 4;
            } else if (corp.name().equals("Thorgate") && project == StandardProject.POWER_PLANT) {
                finalCost -= 3;
            }
        }

        return Math.max(0, finalCost);
    }
}
