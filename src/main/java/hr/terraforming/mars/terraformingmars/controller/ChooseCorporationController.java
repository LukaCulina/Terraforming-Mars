package hr.terraforming.mars.terraformingmars.controller;

import hr.terraforming.mars.terraformingmars.enums.PlayerType;
import hr.terraforming.mars.terraformingmars.model.*;
import hr.terraforming.mars.terraformingmars.network.GameClientThread;
import hr.terraforming.mars.terraformingmars.network.GameServerThread;
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
import lombok.extern.slf4j.Slf4j;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class ChooseCorporationController {

    @FXML private HBox corporationButtonsContainer;
    @FXML private Label chooseCorpLabel;
    @FXML private Button confirmButton;

    private GameManager gameManager;
    private List<Corporation> options;
    private Corporation selectedCorporation;
    private VBox selectedCard;

    public void setCorporationOptions(Player player, List<Corporation> offer, GameManager gameManager) {
        this.gameManager = gameManager;

        String myPlayerName = ApplicationConfiguration.getInstance().getMyPlayerName();

        if (myPlayerName != null && !player.getName().equals(myPlayerName)) {
            showWaitingForPlayer(player.getName());
        } else {
            showCorporationSelection(player, offer);
        }
    }

    private void showWaitingForPlayer(String playerName) {
        chooseCorpLabel.setText("");
        chooseCorpLabel.getStyleClass().clear();

        chooseCorpLabel.setText("Waiting for " + playerName + " to choose their corporation...");
        chooseCorpLabel.getStyleClass().add("waiting-text");

        corporationButtonsContainer.getChildren().clear();
        confirmButton.setVisible(false);
        confirmButton.setManaged(false);

        log.info("Waiting for {} to choose corporation", playerName);
    }

    private void showCorporationSelection(Player player, List<Corporation> offer) {
        chooseCorpLabel.setText(player.getName() + ", choose your corporation:");
        chooseCorpLabel.setStyle("");

        this.options = offer;
        this.selectedCorporation = null;
        this.selectedCard = null;

        populateCorporationBoxes();
        updateConfirmButton();

        confirmButton.setVisible(true);
        confirmButton.setManaged(true);

        log.info("{} is choosing corporation", player.getName());
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

        VBox.setVgrow(contentWrapper, Priority.ALWAYS);

        card.setOnMouseClicked(_ -> selectCorporationCard(corp, card));

        card.getChildren().addAll(contentWrapper);

        return card;
    }

    private void selectCorporationCard(Corporation corp, VBox card) {
        if (selectedCard != null) {
            selectedCard.getStyleClass().remove("card-view-selected");
        }

        selectedCard = card;
        selectedCorporation = corp;

        card.getStyleClass().add("card-view-selected");

        updateConfirmButton();
    }

    @FXML
    private void confirmSelection() {
        if (selectedCorporation == null) {
            return;
        }

        gameManager.assignCorporationAndAdvance(selectedCorporation);
        PlayerType playerType = ApplicationConfiguration.getInstance().getPlayerType();

        if (playerType == PlayerType.HOST) {
            GameServerThread server = ApplicationConfiguration.getInstance().getGameServer();
            if (server != null) {
                server.broadcastGameState(new GameState(gameManager, gameManager.getGameBoard()));
            }
        } else if (playerType == PlayerType.CLIENT) {
            GameClientThread client = ApplicationConfiguration.getInstance().getGameClient();
            if (client != null) {
                client.sendCorporationChoice(selectedCorporation.name());
            }
        }
    }

    private void updateConfirmButton() {
        if (confirmButton != null) {
            confirmButton.setDisable(selectedCorporation == null);
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