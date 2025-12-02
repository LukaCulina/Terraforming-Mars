package hr.terraforming.mars.terraformingmars.view;

import hr.terraforming.mars.terraformingmars.enums.TagType;
import hr.terraforming.mars.terraformingmars.model.Card;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.*;

public final class CardViewBuilder {

    private CardViewBuilder() {
        throw new IllegalStateException("Utility class");
    }

    private static final double CARD_WIDTH = 180.0;
    private static final double CARD_HEIGHT = 220.0;

    public static void setupCardTilePane(TilePane tilePane, int minColumns, int maxColumns) {
        tilePane.setPrefTileWidth(CARD_WIDTH);
        tilePane.setPrefTileHeight(CARD_HEIGHT);
        tilePane.setHgap(5);
        tilePane.setVgap(5);

        tilePane.prefColumnsProperty().bind(
                Bindings.createIntegerBinding(() -> {
                    double width = tilePane.getWidth();
                    if (width < 100) return minColumns;

                    int cols = (int) ((width + 5) / (CARD_WIDTH + 5));

                    return Math.clamp(cols, minColumns, maxColumns);
                }, tilePane.widthProperty())
        );
    }

    public static VBox createCardNode(Card card) {
        VBox cardBox = new VBox(5);
        cardBox.setAlignment(Pos.TOP_CENTER);
        cardBox.getStyleClass().add("card-view");

        cardBox.setPrefSize(CARD_WIDTH, CARD_HEIGHT);
        cardBox.setMinSize(CARD_WIDTH, CARD_HEIGHT);
        cardBox.setMaxSize(CARD_WIDTH, CARD_HEIGHT);

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
