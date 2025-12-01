package hr.terraforming.mars.terraformingmars.view;

import hr.terraforming.mars.terraformingmars.enums.TagType;
import hr.terraforming.mars.terraformingmars.model.Card;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.value.ObservableValue;
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

    private static final double GAP = 5.0;
    private static final double SCROLLBAR_OFFSET = 20.0;
    private static final double MIN_CARD_WIDTH = 120.0;

    public static DoubleBinding createWidthBinding(ReadOnlyDoubleProperty containerWidthProperty, int minColumns) {
        return Bindings.createDoubleBinding(() -> {
            double width = containerWidthProperty.get() - SCROLLBAR_OFFSET;

            if (width < MIN_CARD_WIDTH) return 180.0;

            int cols = minColumns;
            if (width > 1600) cols = minColumns + 3;
            else if (width > 1200) cols = minColumns + 2;
            else if (width > 800) cols = minColumns + 1;

            double cardW = (width - (cols - 1) * GAP) / cols;
            return Math.floor(Math.max(cardW, MIN_CARD_WIDTH));

        }, containerWidthProperty);
    }


    public static VBox createCardNode(Card card, ObservableValue<? extends Number> cardWidthProperty) {
        VBox cardBox = new VBox(5);
        cardBox.setAlignment(Pos.TOP_CENTER);
        cardBox.getStyleClass().add("card-view");

        if (cardWidthProperty != null) {
            cardBox.prefWidthProperty().bind(cardWidthProperty);
            cardBox.prefHeightProperty().bind(cardBox.prefWidthProperty().multiply(1.3));

            cardBox.styleProperty().bind(Bindings.createStringBinding(() -> {
                double w = cardBox.getPrefWidth();
                return String.format("-fx-font-size: %.1fpx;", Math.clamp(w * 0.08, 11.0, 24.0));
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
