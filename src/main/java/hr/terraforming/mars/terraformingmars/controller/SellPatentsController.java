package hr.terraforming.mars.terraformingmars.controller;

import hr.terraforming.mars.terraformingmars.model.Card;
import hr.terraforming.mars.terraformingmars.model.Player;
import hr.terraforming.mars.terraformingmars.view.CardViewBuilder;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SellPatentsController {

    private static final Logger logger = LoggerFactory.getLogger(SellPatentsController.class);

    @FXML private TilePane cardsForSalePane;
    @FXML private Label infoLabel;
    @FXML private Button confirmButton;
    @FXML private Button cancelButton;

    private Player player;
    private final Set<Card> selectedCards = new HashSet<>();
    private Consumer<List<Card>> onSaleComplete;
    private static final String SELECTED_CARD_STYLE = "card-view-selected";

    public void initData(Player player, Consumer<List<Card>> onSaleComplete) {
        this.player = player;
        this.onSaleComplete = onSaleComplete;
        populateCards();
        updateInfoLabel();
    }

    private void populateCards() {
        cardsForSalePane.getChildren().clear();
        for (Card card : player.getHand()) {
            VBox cardNode = CardViewBuilder.createCardNode(card);
            cardNode.setOnMouseClicked(_ -> toggleCardSelection(card, cardNode));
            cardsForSalePane.getChildren().add(cardNode);
        }
    }

    private void toggleCardSelection(Card card, VBox cardNode) {
        if (selectedCards.contains(card)) {
            selectedCards.remove(card);
            cardNode.getStyleClass().remove(SELECTED_CARD_STYLE);
        } else {
            selectedCards.add(card);
            cardNode.getStyleClass().add(SELECTED_CARD_STYLE);
        }
        updateInfoLabel();
    }

    private void updateInfoLabel() {
        int count = selectedCards.size();
        infoLabel.setText("Selected: " + count + " cards for " + count + " MC");
        confirmButton.setDisable(count == 0);
    }

    @FXML
    private void confirmSale() {
        if (!selectedCards.isEmpty()) {
            int cardsSoldCount = selectedCards.size();

            player.getHand().removeAll(selectedCards);

            player.addMC(cardsSoldCount);

            logger.info("{} sold {} patent(s) for {} MC.", player.getName(), selectedCards.size(), selectedCards.size());
            if (onSaleComplete != null) {
                onSaleComplete.accept(new ArrayList<>(selectedCards));
            }

            closeWindow();
        }
    }

    @FXML
    private void cancel() {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) cardsForSalePane.getScene().getWindow();
        if (stage != null) {
            stage.close();
        }
    }

    public void replayShowSoldPatents(List<String> soldCardNames, List<Card> handBeforeSale) {
        infoLabel.setText("Replay: Sold " + soldCardNames.size() + " card(s)");
        confirmButton.setVisible(false);
        cancelButton.setVisible(false);

        cardsForSalePane.getChildren().clear();

        for (Card card : handBeforeSale) {
            VBox cardNode = CardViewBuilder.createCardNode(card);
            cardNode.setMouseTransparent(true);

            if (soldCardNames.contains(card.getName())) {
                cardNode.getStyleClass().add(SELECTED_CARD_STYLE);
                cardNode.setOpacity(1.0);
            } else {
                cardNode.setOpacity(0.4);
            }

            cardsForSalePane.getChildren().add(cardNode);
        }

        PauseTransition autoClose = new PauseTransition(Duration.seconds(2));
        autoClose.setOnFinished(_ -> closeWindow());
        autoClose.play();
    }
}
