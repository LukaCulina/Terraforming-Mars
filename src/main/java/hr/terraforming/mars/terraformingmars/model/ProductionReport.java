package hr.terraforming.mars.terraformingmars.model;

import lombok.Data;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class ProductionReport implements Serializable {
    private String playerName;
    private String corporationName;

    private int megaCreditsBefore;
    private int megaCreditsAfter;
    private int megaCreditsProduction;

    private int plantsBefore;
    private int plantsAfter;
    private int plantsProduction;

    private int heatBefore;
    private int heatAfter;
    private int heatProduction;

    private int energyBefore;
    private int energyAfter;
    private int energyProduction;

    private int energyConvertedToHeat;

    private static final String FORMAT = "+%d (%d → %d)";

    public Map<String, String> getChanges() {
        Map<String, String> changes = new LinkedHashMap<>();


        if (megaCreditsProduction > 0) {
            changes.put("MegaCredits",
                    String.format(FORMAT,
                            megaCreditsProduction, megaCreditsBefore, megaCreditsAfter));
        }

        if (energyProduction > 0 || energyBefore != energyAfter) {
            changes.put("Energy",
                    String.format(FORMAT,
                            energyProduction, energyBefore, energyAfter));
        }

        if (energyConvertedToHeat > 0) {
            changes.put("Energy → Heat",
                    String.format("+%d converted", energyConvertedToHeat));
        }

        if (heatProduction > 0) {
            changes.put("Heat",
                    String.format(FORMAT,
                            heatProduction, heatBefore, heatAfter));
        }

        if (plantsProduction > 0) {
            changes.put("Plants",
                    String.format(FORMAT,
                            plantsProduction, plantsBefore, plantsAfter));
        }

        if (changes.isEmpty()) {
            changes.put("ℹ️", "No production this turn");
        }

        return changes;
    }
}
