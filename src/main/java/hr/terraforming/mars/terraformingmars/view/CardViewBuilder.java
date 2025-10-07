package hr.terraforming.mars.terraformingmars.view;

import hr.terraforming.mars.terraformingmars.enums.TagType;
import hr.terraforming.mars.terraformingmars.model.Card;
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

    public static VBox createCardNode(Card card) {
        VBox cardBox = new VBox(5);
        cardBox.setAlignment(Pos.TOP_CENTER);
        cardBox.setPrefSize(200, 245);
        cardBox.getStyleClass().add("card-view");

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

        if (card.getVictoryPoints() > 0) {
            Label vpLabel = new Label(String.valueOf(card.getVictoryPoints()));
            vpLabel.getStyleClass().add("vp-label");

            StackPane vpContainer = new StackPane(vpLabel);
            vpContainer.setAlignment(Pos.BOTTOM_RIGHT);
            vpContainer.setPadding(new Insets(0, 10, 10, 0));

            cardBox.getChildren().add(vpContainer);
        }

        return cardBox;
    }

    public static Region createTagNode(TagType tag) {
        Region tagNode = new Region();
        tagNode.getStyleClass().add("tag-icon");
        tagNode.getStyleClass().add("tag-" + tag.name().toLowerCase());
        return tagNode;
    }
}
