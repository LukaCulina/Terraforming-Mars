package hr.terraforming.mars.terraformingmars.controller;

import hr.terraforming.mars.terraformingmars.enums.PlayerType;
import hr.terraforming.mars.terraformingmars.model.*;
import hr.terraforming.mars.terraformingmars.network.GameClientThread;
import hr.terraforming.mars.terraformingmars.network.NetworkBroadcaster;
import hr.terraforming.mars.terraformingmars.view.CorporationViewBuilder;
import hr.terraforming.mars.terraformingmars.view.GameScreens;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;
import java.util.List;

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
            VBox corpCard = CorporationViewBuilder.createCorporationNode(corp);

            corpCard.setOnMouseClicked(_ -> selectCorporationCard(corp, corpCard));

            corporationButtonsContainer.getChildren().add(corpCard);
        }
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

        switch (playerType) {
            case PlayerType.HOST -> {
                NetworkBroadcaster broadcaster = ApplicationConfiguration.getInstance().getBroadcaster();
                if (broadcaster != null) {
                    broadcaster.broadcast();
                }
            }
            case PlayerType.CLIENT -> {
                GameClientThread client = ApplicationConfiguration.getInstance().getGameClient();
                if (client != null) {
                    client.sendCorporationChoice(selectedCorporation.name());
                }
            }
            default -> {
                boolean allChosen = gameManager.getPlayers().stream()
                        .allMatch(p -> p.getCorporation() != null);

                if (allChosen) {
                    GameScreens.showInitialCardDraftScreen(gameManager);
                } else {
                    GameScreens.showChooseCorporationScreen(gameManager);
                }
            }
        }
    }

    private void updateConfirmButton() {
        if (confirmButton != null) {
            confirmButton.setDisable(selectedCorporation == null);
        }
    }
}