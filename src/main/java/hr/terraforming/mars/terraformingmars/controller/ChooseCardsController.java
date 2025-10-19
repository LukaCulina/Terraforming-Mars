package hr.terraforming.mars.terraformingmars.controller;

import hr.terraforming.mars.terraformingmars.factory.CardFactory;
import hr.terraforming.mars.terraformingmars.model.Card;
import hr.terraforming.mars.terraformingmars.model.Player;
import hr.terraforming.mars.terraformingmars.view.CardViewBuilder;
import javafx.animation.PauseTransition;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
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
    @FXML private Button confirmButton;

    private static final String SELECTED_CARD_STYLE = "card-view-selected";

    private Player player;
    private final Set<Card> selectedCards = new HashSet<>();
    private final IntegerProperty remainingMC = new SimpleIntegerProperty();

    private Consumer<List<Card>> onConfirm;

    public void setup(Player player, List<Card> offer, Consumer<List<Card>> onConfirm) {
        this.player = player;
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

        final int costPerCard = 3;

        if (cardNode.getStyleClass().contains(SELECTED_CARD_STYLE)) {
            selectedCards.remove(card);
            cardNode.getStyleClass().remove(SELECTED_CARD_STYLE);
        } else {
            int potentialCost = (selectedCards.size() + 1) * costPerCard;

            if (player.getMC() >= potentialCost) {
                selectedCards.add(card);
                cardNode.getStyleClass().add(SELECTED_CARD_STYLE);
            } else {
                logger.warn("Player {} failed to select card '{}': not enough MC (has {}, needs {}).",
                        player.getName(), card.getName(), player.getMC(), potentialCost);
            }
        }

        int totalCost = selectedCards.size() * costPerCard;
        remainingMC.set(player.getMC() - totalCost);
    }

    @FXML
    private void confirmSelection() {
        List<Card> boughtCards = new ArrayList<>(selectedCards);

        if (onConfirm != null) {
            onConfirm.accept(boughtCards);
        }
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) cardsTile.getScene().getWindow();
        if (stage != null) {
            stage.close();
        }
    }

    public void replayShowChosenCards(List<String> boughtCardNames) {
        chooseCardsLabel.setText("Replay: Player bought " + boughtCardNames.size() + " card(s)");
        remainingMCLabel.setText("");

        cardsTile.getChildren().clear();
        for (String cardName : boughtCardNames) {
            Card card = CardFactory.getCardByName(cardName);
            if (card != null) {
                VBox cardNode = CardViewBuilder.createCardNode(card);
                cardNode.setMouseTransparent(true);
                cardNode.getStyleClass().add(SELECTED_CARD_STYLE);
                cardsTile.getChildren().add(cardNode);
            }
        }

        confirmButton.setVisible(false);

        PauseTransition autoClose = new PauseTransition(Duration.seconds(1.5));
        autoClose.setOnFinished(_ -> closeWindow());
        autoClose.play();
    }
}
