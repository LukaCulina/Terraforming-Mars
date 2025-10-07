package hr.terraforming.mars.terraformingmars.controller;

import hr.terraforming.mars.terraformingmars.model.Card;
import hr.terraforming.mars.terraformingmars.model.Player;
import hr.terraforming.mars.terraformingmars.view.CardViewBuilder;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SellPatentsController {

    private static final Logger logger = LoggerFactory.getLogger(SellPatentsController.class);

    @FXML private TilePane cardsForSalePane;
    @FXML private Label infoLabel;
    @FXML private Button confirmButton;

    private Player player;
    private final Set<Card> selectedCards = new HashSet<>();
    private Runnable onSaleComplete;

    public void initData(Player player, Runnable onSaleComplete) {
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
            cardNode.getStyleClass().remove("card-view-selected");
        } else {
            selectedCards.add(card);
            cardNode.getStyleClass().add("card-view-selected");
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
            player.getHand().removeAll(selectedCards);

            player.addMC(selectedCards.size());

            logger.info("{} sold {} patent(s) for {} MC.", player.getName(), selectedCards.size(), selectedCards.size());
        }

        if (onSaleComplete != null) {
            onSaleComplete.run();
        }

        closeWindow();
    }

    @FXML
    private void cancel() {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) cardsForSalePane.getScene().getWindow();
        stage.close();
    }
}
