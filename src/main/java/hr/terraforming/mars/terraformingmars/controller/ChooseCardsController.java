package hr.terraforming.mars.terraformingmars.controller;

import hr.terraforming.mars.terraformingmars.enums.CardSelectionContext;
import hr.terraforming.mars.terraformingmars.model.Card;
import hr.terraforming.mars.terraformingmars.model.Player;
import hr.terraforming.mars.terraformingmars.view.CardViewBuilder;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class ChooseCardsController {

    private static final Logger logger = LoggerFactory.getLogger(ChooseCardsController.class);

    @FXML private TilePane cardsTile;
    @FXML private Label remainingMCLabel;
    @FXML private Label chooseCardsLabel;

    private static final String SELECTED_CARD_STYLE = "card-view-selected";

    private Player player;
    private final Set<Card> selectedCards = new HashSet<>();
    private final IntegerProperty remainingMC = new SimpleIntegerProperty();
    private CardSelectionContext context;

    private Consumer<List<Card>> onConfirm;

    public void setup(Player player, List<Card> offer, CardSelectionContext context, Consumer<List<Card>> onConfirm) {
        this.player = player;
        this.context = context;
        this.onConfirm = onConfirm;

        chooseCardsLabel.setText(player.getName() + ", choose your cards:");
        this.selectedCards.clear();

        remainingMCLabel.textProperty().bind(remainingMC.asString("Remaining MC: %d"));
        remainingMC.set(player.getMC());

        cardsTile.getChildren().clear();
        for (Card card : offer) {
            VBox cardNode = CardViewBuilder.createCardNode(card);
            cardNode.setOnMouseClicked(_ -> toggleSelection(card, cardNode));
            cardsTile.getChildren().add(cardNode);
        }
    }



    private void toggleSelection(Card card, VBox cardNode) {
        if (cardNode.getStyleClass().contains(SELECTED_CARD_STYLE)) {
            selectedCards.remove(card);
            cardNode.getStyleClass().remove(SELECTED_CARD_STYLE);
        } else {
            if (context == CardSelectionContext.DRAFT || player.getMC() >= (selectedCards.size() + 1) * 3) {
                selectedCards.add(card);
                cardNode.getStyleClass().add(SELECTED_CARD_STYLE);
            } else {
                logger.warn("Player {} failed to select card '{}': not enough MC (has {}, needs {}).",
                        player.getName(), card.getName(), player.getMC(), (selectedCards.size() + 1) * 3);
            }
        }

        int costPerCard = (context == CardSelectionContext.RESEARCH) ? 3 : 0;
        int totalCost = selectedCards.size() * costPerCard;
        remainingMC.set(player.getMC() - totalCost);
    }

    @FXML
    private void confirmSelection() {
        if (onConfirm != null) {
            onConfirm.accept(new ArrayList<>(selectedCards));
        }
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) cardsTile.getScene().getWindow();
        if (stage != null) {
            stage.close();
        }
    }
}
