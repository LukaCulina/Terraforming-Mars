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

    public static List<ProductionReport> generateReports(GameManager gameManager) {
        List<ProductionReport> summaries = new ArrayList<>();

        for (Player player : gameManager.getPlayers()) {
            ProductionReport report = createReportForPlayer(player);
            summaries.add(report);
        }

        return summaries;
    }

    public static ProductionReport createReportForPlayer(Player player) {
        ProductionReport report = new ProductionReport();

        report.setPlayerName(player.getName());
        report.setCorporationName(
                player.getCorporation() != null
                        ? player.getCorporation().name()
                        : "No Corporation"
        );

        report.setMegaCreditsBefore(player.getMC());
        report.setEnergyBefore(player.resourceProperty(ResourceType.ENERGY).get());
        report.setHeatBefore(player.resourceProperty(ResourceType.HEAT).get());
        report.setPlantsBefore(player.resourceProperty(ResourceType.PLANTS).get());

        report.setMegaCreditsProduction(
                player.getTR() + player.productionProperty(ResourceType.MEGA_CREDITS).get()
        );
        report.setEnergyProduction(player.productionProperty(ResourceType.ENERGY).get());
        report.setHeatProduction(player.productionProperty(ResourceType.HEAT).get());
        report.setPlantsProduction(player.productionProperty(ResourceType.PLANTS).get());

        int energyBefore = player.resourceProperty(ResourceType.ENERGY).get();
        report.setEnergyConvertedToHeat(energyBefore);

        report.setMegaCreditsAfter(
                report.getMegaCreditsBefore() + report.getMegaCreditsProduction()
        );
        report.setEnergyAfter(report.getEnergyProduction());
        report.setHeatAfter(
                report.getHeatBefore() + report.getHeatProduction() + energyBefore
        );
        report.setPlantsAfter(
                report.getPlantsBefore() + report.getPlantsProduction()
        );

        return report;
    }
}
