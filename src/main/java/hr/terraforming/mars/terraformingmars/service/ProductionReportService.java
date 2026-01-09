package hr.terraforming.mars.terraformingmars.service;

import hr.terraforming.mars.terraformingmars.enums.ResourceType;
import hr.terraforming.mars.terraformingmars.model.GameManager;
import hr.terraforming.mars.terraformingmars.model.Player;
import hr.terraforming.mars.terraformingmars.model.ProductionReport;

import java.util.ArrayList;
import java.util.List;

public class ProductionReportService {

    private ProductionReportService() {
        throw new IllegalStateException("Utility class");
    }

    public static List<ProductionReport> generateSummaries(GameManager gameManager) {
        List<ProductionReport> summaries = new ArrayList<>();

        for (Player player : gameManager.getPlayers()) {
            ProductionReport summary = createSummaryForPlayer(player);
            summaries.add(summary);
        }

        return summaries;
    }

    public static ProductionReport createSummaryForPlayer(Player player) {
        ProductionReport summary = new ProductionReport();

        summary.setPlayerName(player.getName());
        summary.setCorporationName(
                player.getCorporation() != null
                        ? player.getCorporation().name()
                        : "No Corporation"
        );

        summary.setMegaCreditsBefore(player.getMC());
        summary.setEnergyBefore(player.resourceProperty(ResourceType.ENERGY).get());
        summary.setHeatBefore(player.resourceProperty(ResourceType.HEAT).get());
        summary.setPlantsBefore(player.resourceProperty(ResourceType.PLANTS).get());

        summary.setMegaCreditsProduction(
                player.getTR() + player.productionProperty(ResourceType.MEGA_CREDITS).get()
        );
        summary.setEnergyProduction(player.productionProperty(ResourceType.ENERGY).get());
        summary.setHeatProduction(player.productionProperty(ResourceType.HEAT).get());
        summary.setPlantsProduction(player.productionProperty(ResourceType.PLANTS).get());

        int energyBefore = player.resourceProperty(ResourceType.ENERGY).get();
        summary.setEnergyConvertedToHeat(energyBefore);

        summary.setMegaCreditsAfter(
                summary.getMegaCreditsBefore() + summary.getMegaCreditsProduction()
        );
        summary.setEnergyAfter(summary.getEnergyProduction());
        summary.setHeatAfter(
                summary.getHeatBefore() + summary.getHeatProduction() + energyBefore
        );
        summary.setPlantsAfter(
                summary.getPlantsBefore() + summary.getPlantsProduction()
        );

        return summary;
    }
}
