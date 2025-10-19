package hr.terraforming.mars.terraformingmars.controller;

import hr.terraforming.mars.terraformingmars.enums.ResourceType;
import hr.terraforming.mars.terraformingmars.enums.TagType;
import hr.terraforming.mars.terraformingmars.model.Card;
import hr.terraforming.mars.terraformingmars.model.Player;
import hr.terraforming.mars.terraformingmars.view.CardViewBuilder;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;

import java.util.EnumSet;
import java.util.List;
import java.util.function.Consumer;

public class PlayerBoardController {

    @FXML private Label corporationLabel;
    @FXML private Label trLabel;
    @FXML private Label mcLabel;
    @FXML private Label steelLabel;
    @FXML private Label titaniumLabel;
    @FXML private Label plantsLabel;
    @FXML private Label energyLabel;
    @FXML private Label heatLabel;
    @FXML private Label mcProductionLabel;
    @FXML private Label steelProductionLabel;
    @FXML private Label titaniumProductionLabel;
    @FXML private Label plantsProductionLabel;
    @FXML private Label energyProductionLabel;
    @FXML private Label heatProductionLabel;
    @FXML private FlowPane tagsLegendPane;
    @FXML private VBox handContainer;
    @FXML private TilePane cardsDisplayArea;
    @FXML private Button showHandButton;
    @FXML private Button showPlayedButton;

    private Player player;
    private Consumer<Card> cardPlayHandler;

    private boolean isShowingHand = true;

    public void setPlayer(Player player, Consumer<Card> cardPlayHandler) {
        this.player = player;
        this.cardPlayHandler = cardPlayHandler;
        if (player == null) return;

        updatePlayerInfo();
        updateCardsDisplay();
    }

    private void updatePlayerInfo() {
        corporationLabel.setText("Corporation: " + (player.getCorporation() != null ? player.getCorporation().name() : "N/A"));

        trLabel.textProperty().bind(player.trProperty().asString("TR: %d"));
        mcLabel.textProperty().bind(player.mcProperty().asString());
        steelLabel.textProperty().bind(player.resourceProperty(ResourceType.STEEL).asString());
        titaniumLabel.textProperty().bind(player.resourceProperty(ResourceType.TITANIUM).asString());
        plantsLabel.textProperty().bind(player.resourceProperty(ResourceType.PLANTS).asString());
        energyLabel.textProperty().bind(player.resourceProperty(ResourceType.ENERGY).asString());
        heatLabel.textProperty().bind(player.resourceProperty(ResourceType.HEAT).asString());

        mcProductionLabel.textProperty().bind(player.productionProperty(ResourceType.MEGACREDITS).asString());
        steelProductionLabel.textProperty().bind(player.productionProperty(ResourceType.STEEL).asString());
        titaniumProductionLabel.textProperty().bind(player.productionProperty(ResourceType.TITANIUM).asString());
        plantsProductionLabel.textProperty().bind(player.productionProperty(ResourceType.PLANTS).asString());
        energyProductionLabel.textProperty().bind(player.productionProperty(ResourceType.ENERGY).asString());
        heatProductionLabel.textProperty().bind(player.productionProperty(ResourceType.HEAT).asString());
    }

    @FXML
    private void showHandCardsView() {
        isShowingHand = true;
        updateCardsDisplay();
    }

    @FXML
    private void showPlayedCardsView() {
        isShowingHand = false;
        updateCardsDisplay();
    }

    private void updateCardsDisplay() {
        if (player == null || cardsDisplayArea == null) return;

        cardsDisplayArea.getChildren().clear();

        List<Card> cardsToShow = isShowingHand ? player.getHand() : player.getPlayed();
        final String disabledClass = "card-view-disabled";

        cardsToShow.forEach(card -> {
            VBox cardNode = CardViewBuilder.createCardNode(card);

            if (isShowingHand) {
                boolean canPlay = player.canPlayCard(card);

                if (canPlay && cardPlayHandler != null) {
                    cardNode.setOnMouseClicked(_ -> cardPlayHandler.accept(card));
                    cardNode.getStyleClass().remove(disabledClass);
                    cardNode.setDisable(false);
                } else {
                    cardNode.setOnMouseClicked(null);
                    cardNode.getStyleClass().add(disabledClass);
                    cardNode.setDisable(true);
                }
            } else {
                cardNode.setOnMouseClicked(null);
            }

            cardsDisplayArea.getChildren().add(cardNode);
        });

        updateTagsLegend();
        updateCardViewButtons();
    }

    private void updateCardViewButtons() {
        final String activeClass = "card-button-active";
        showHandButton.getStyleClass().remove(activeClass);
        showPlayedButton.getStyleClass().remove(activeClass);

        if (isShowingHand) {
            showHandButton.getStyleClass().add(activeClass);
        } else {
            showPlayedButton.getStyleClass().add(activeClass);
        }
    }

    private void updateTagsLegend() {
        if (player == null || tagsLegendPane == null) return;
        tagsLegendPane.getChildren().clear();

        for (TagType tag : EnumSet.allOf(TagType.class)) {
            int count = player.countTags(tag);
            HBox tagEntry = new HBox(5);
            tagEntry.setAlignment(Pos.CENTER_LEFT);

            Label countLabel = new Label(String.valueOf(count));
            countLabel.getStyleClass().add("tag-text-label");

            Region tagNode = CardViewBuilder.createTagNode(tag);

            String tagName = tag.name().substring(0, 1).toUpperCase() + tag.name().substring(1).toLowerCase();
            Label nameLabel = new Label(tagName);
            nameLabel.getStyleClass().add("tag-text-label");

            tagEntry.getChildren().addAll(countLabel, tagNode, nameLabel);
            tagsLegendPane.getChildren().add(tagEntry);
        }
    }

    public void setHandInteractionEnabled(boolean isEnabled) {
        if (handContainer != null) {
            handContainer.setDisable(!isEnabled);
        }
    }
}