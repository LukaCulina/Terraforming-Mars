package hr.terraforming.mars.terraformingmars.view;

import hr.terraforming.mars.terraformingmars.enums.ResourceType;
import hr.terraforming.mars.terraformingmars.model.Corporation;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.concurrent.atomic.AtomicInteger;

public class CorporationViewBuilder {

    private CorporationViewBuilder() {
        throw new IllegalStateException("Utility class");
    }

    public static VBox createCorporationNode(Corporation corp) {
        VBox card = new VBox();
        card.getStyleClass().add("corporation-card");
        card.setAlignment(Pos.TOP_CENTER);
        card.setPadding(new Insets(20));
        card.setPrefSize(320, 450);
        card.setMaxSize(320, 450);

        VBox contentWrapper = new VBox(15);
        contentWrapper.setAlignment(Pos.TOP_CENTER);

        Label nameLabel = new Label(corp.name().toUpperCase());
        nameLabel.getStyleClass().add("corporation-name");

        VBox resourcesSection = new VBox(10);
        Label resourcesTitle = new Label("Initial State:");
        resourcesTitle.getStyleClass().add("section-title");

        GridPane grid = new GridPane();
        grid.getStyleClass().add("resources-grid");
        grid.setHgap(15);
        grid.setVgap(8);

        AtomicInteger row = new AtomicInteger(0);
        grid.add(new Label("💰 Starting MC: " + corp.startingMC()), 0, row.getAndIncrement());

        if (corp.startingResources() != null && !corp.startingResources().isEmpty()) {
            corp.startingResources().forEach((type, amount) ->
                    grid.add(new Label(getResourceIcon(type) + " " + amount + " " + formatResourceName(type.name())), 0, row.getAndIncrement()));
        }

        if (corp.startingProduction() != null && !corp.startingProduction().isEmpty()) {
            grid.add(new Label("⬆Production:"), 0, row.getAndIncrement());
            corp.startingProduction().forEach((_, amount) ->
                    grid.add(new Label("   + " + amount + "/gen"), 0, row.getAndIncrement()));
        }
        resourcesSection.getChildren().addAll(resourcesTitle, grid);

        VBox abilitySection = new VBox(5);
        Label abilityTitle = new Label("Ability:");
        abilityTitle.getStyleClass().add("section-title");

        Label abilityLabel = new Label(corp.abilityDescription());
        abilityLabel.getStyleClass().add("ability-text");
        abilityLabel.setWrapText(true);
        abilityLabel.setAlignment(Pos.CENTER);
        abilityLabel.setMaxWidth(260);
        abilitySection.getChildren().addAll(abilityTitle, abilityLabel);

        contentWrapper.getChildren().addAll(nameLabel, resourcesSection, abilitySection);

        VBox.setVgrow(contentWrapper, Priority.ALWAYS);
        card.getChildren().addAll(contentWrapper);

        return card;
    }

    private static String getResourceIcon(ResourceType type) {
        return switch (type) {
            case STEEL -> "🔩";
            case TITANIUM -> "🚀";
            case PLANTS -> "🌿";
            case ENERGY -> "⚡";
            case HEAT -> "🔥";
            default -> "";
        };
    }

    private static String formatResourceName(String name) {
        if (name == null || name.isEmpty()) return "";
        return name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
    }
}
