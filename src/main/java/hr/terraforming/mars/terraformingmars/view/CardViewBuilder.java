package hr.terraforming.mars.terraformingmars.view;

import hr.terraforming.mars.terraformingmars.enums.TagType;
import hr.terraforming.mars.terraformingmars.model.Card;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public final class CardViewBuilder {

    private CardViewBuilder() {
        throw new IllegalStateException("Utility class");
    }

    public static VBox createCardNode(Card card, ReadOnlyDoubleProperty parentWidthProperty) {
        VBox cardBox = new VBox(5);
        cardBox.setAlignment(Pos.TOP_CENTER);
        cardBox.getStyleClass().add("card-view");

        if (parentWidthProperty != null) {
            final double MIN_CARD_WIDTH = 120.0;
            final double GAP = 5.0;
            final double SCROLLBAR_OFFSET = 20.0;

            var dynamicWidthBinding = Bindings.createDoubleBinding(() -> {

                double containerWidth = parentWidthProperty.get() - SCROLLBAR_OFFSET;

                int targetColumns;
                if (containerWidth > 1600) targetColumns = 8;
                else if (containerWidth > 1200) targetColumns = 7;
                else if (containerWidth > 800) targetColumns = 5;
                else targetColumns = 3;

                double exactWidth = (containerWidth - (targetColumns - 1) * GAP) / targetColumns;

                if (exactWidth < 120.0) exactWidth = 120.0;

                return Math.floor(exactWidth) - 1.0;

            }, parentWidthProperty);

            cardBox.prefWidthProperty().bind(dynamicWidthBinding);

            cardBox.prefHeightProperty().bind(cardBox.prefWidthProperty().multiply(1.4));

            cardBox.setMinWidth(MIN_CARD_WIDTH);
            cardBox.setMinHeight(MIN_CARD_WIDTH * 1.3);

            cardBox.styleProperty().bind(Bindings.createStringBinding(() -> {
                double cardWidth = cardBox.getPrefWidth();

                double idealFontSize = cardWidth * 0.08;

                double actualFontSize = Math.clamp(idealFontSize, 11.0, 24.0);

                return String.format("-fx-font-size: %.1fpx;", actualFontSize);
            }, cardBox.prefWidthProperty()));

        } else {
            cardBox.setPrefSize(180, 220);
        }

        Label costLabel = new Label(card.getCost() + " MC");
        costLabel.getStyleClass().add("card-cost-label");

        Label nameLabel = new Label(card.getName());
        nameLabel.getStyleClass().add("card-name-label");

        HBox tagBox = new HBox(5);
        tagBox.setAlignment(Pos.CENTER);
        for (TagType tag : card.getTags()) {
            Region tagNode = createTagNode(tag);
            tagBox.getChildren().add(tagNode);
        }

        Label descLabel = new Label(card.getDescription());
        descLabel.setWrapText(true);
        descLabel.getStyleClass().add("card-description-label");

        VBox descriptionWrapper = new VBox(descLabel);
        descriptionWrapper.setAlignment(Pos.CENTER);
        VBox.setVgrow(descriptionWrapper, Priority.ALWAYS);

        cardBox.getChildren().addAll(costLabel, nameLabel, tagBox, descriptionWrapper);

        Label vpLabel = new Label(String.valueOf(card.getVictoryPoints()));
        vpLabel.getStyleClass().add("vp-label");

        StackPane vpContainer = new StackPane(vpLabel);
        vpContainer.setAlignment(Pos.BOTTOM_RIGHT);
        vpContainer.setPadding(new Insets(0, 3, 3, 0));

        vpContainer.setVisible(card.getVictoryPoints() > 0);
        vpContainer.setManaged(card.getVictoryPoints() > 0);

        cardBox.getChildren().add(vpContainer);

        return cardBox;
    }

    public static Region createTagNode(TagType tag) {
        Region tagNode = new Region();
        tagNode.getStyleClass().add("tag-icon");
        tagNode.getStyleClass().add("tag-" + tag.name().toLowerCase());
        return tagNode;
    }
}
