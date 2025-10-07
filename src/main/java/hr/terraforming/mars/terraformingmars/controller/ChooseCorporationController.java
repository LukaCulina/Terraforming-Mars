package hr.terraforming.mars.terraformingmars.controller;

import hr.terraforming.mars.terraformingmars.view.GameScreens;
import hr.terraforming.mars.terraformingmars.model.Corporation;
import hr.terraforming.mars.terraformingmars.model.GameManager;
import hr.terraforming.mars.terraformingmars.model.Player;
import hr.terraforming.mars.terraformingmars.enums.ResourceType;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class ChooseCorporationController {

    private static final Logger logger = LoggerFactory.getLogger(ChooseCorporationController.class);

    @FXML
    private HBox corporationButtonsContainer;
    @FXML
    private Label chooseCorpLabel;

    private GameManager gameManager;

    private List<Corporation> options;

    public void setCorporationOptions(Player player, List<Corporation> offer, GameManager gameManager) {
        this.gameManager = gameManager;
        chooseCorpLabel.setText(player.getName() + ", choose your corporation:");
        this.options = offer;
        populateCorporationBoxes();
    }

    private void populateCorporationBoxes() {
        corporationButtonsContainer.getChildren().clear();
        for (Corporation corp : options) {
            VBox corpCard = createCorporationNode(corp);
            corporationButtonsContainer.getChildren().add(corpCard);
        }
    }

    private VBox createCorporationNode(Corporation corp) {
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

        Button selectBtn = new Button("Select " + corp.name());
        selectBtn.getStyleClass().add("select-button");

        selectBtn.setOnAction(_ -> {
            logger.info("Player selected corporation '{}' via button click.", corp.name());
            selectCorporation(corp);
        });

        VBox.setVgrow(contentWrapper, Priority.ALWAYS);

        card.setOnMouseClicked(_ -> {
            logger.info("Player selected corporation '{}' via card click.", corp.name());
            selectCorporation(corp);
        });

        card.getChildren().addAll(contentWrapper, selectBtn);

        return card;
    }

    private void selectCorporation(Corporation corp) {
        boolean morePlayersToChoose = gameManager.assignCorporationAndAdvance(corp);

        if (morePlayersToChoose) {
            GameScreens.showChooseCorporationScreen(gameManager);
        } else {
            GameScreens.showInitialCardDraftScreen(gameManager);
        }
    }

    private String getResourceIcon(ResourceType type) {
        return switch (type) {
            case STEEL -> "🔩";
            case TITANIUM -> "🛰️";
            case PLANTS -> "🌿";
            case ENERGY -> "⚡";
            case HEAT -> "🔥";
            default -> "";
        };
    }

    private String formatResourceName(String name) {
        if (name == null || name.isEmpty()) return "";
        return name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
    }
}
