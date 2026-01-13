package hr.terraforming.mars.terraformingmars.controller.game;

import hr.terraforming.mars.terraformingmars.enums.PlayerType;
import hr.terraforming.mars.terraformingmars.model.*;
import hr.terraforming.mars.terraformingmars.network.GameClientThread;
import hr.terraforming.mars.terraformingmars.network.NetworkBroadcaster;
import hr.terraforming.mars.terraformingmars.view.CorporationViewBuilder;
import hr.terraforming.mars.terraformingmars.view.ScreenNavigator;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;
import java.util.List;

@Slf4j
public class ChooseCorporationController {

    @FXML
    private HBox corporationButtonsContainer;

    @FXML
    private Label chooseCorpLabel;

    @FXML
    private Button confirmButton;

    private GameManager gameManager;
    private List<Corporation> availableCorporations;
    private Corporation selectedCorporation;
    private VBox selectedCorporationCard;

    public void setCorporationOptions(Player player, List<Corporation> offer, GameManager gameManager) {
        this.gameManager = gameManager;

        String myPlayerName = ApplicationConfiguration.getInstance().getMyPlayerName();

        if (myPlayerName != null && !player.getName().equals(myPlayerName)) {
            showWaitingForPlayer();
        } else {
            showCorporationSelection(player, offer);
        }
    }

    private void showWaitingForPlayer() {
        chooseCorpLabel.setText("");
        chooseCorpLabel.getStyleClass().clear();

        chooseCorpLabel.setText("Corporation Selected!\nWaiting for other players to finish...");
        chooseCorpLabel.getStyleClass().add("waiting-text");

        corporationButtonsContainer.getChildren().clear();
        confirmButton.setVisible(false);
        confirmButton.setManaged(false);

        log.debug("Transitioned to waiting screen.");
    }

    private void showCorporationSelection(Player player, List<Corporation> offer) {
        chooseCorpLabel.setText(player.getName() + ", choose your corporation:");
        chooseCorpLabel.setStyle("");

        availableCorporations = offer;
        selectedCorporation = null;
        selectedCorporationCard = null;

        populateCorporationBoxes();
        updateConfirmButton();

        confirmButton.setVisible(true);
        confirmButton.setManaged(true);

        log.info("{} is choosing a corporation", player.getName());
    }

    private void populateCorporationBoxes() {
        corporationButtonsContainer.getChildren().clear();

        for (Corporation corp : availableCorporations) {
            VBox corpCard = CorporationViewBuilder.createCorporationNode(corp);

            corpCard.setOnMouseClicked(_ -> selectCorporationCard(corp, corpCard));

            corporationButtonsContainer.getChildren().add(corpCard);
        }
    }

    private void selectCorporationCard(Corporation corp, VBox card) {
        if (selectedCorporationCard != null) {
            selectedCorporationCard.getStyleClass().remove("card-view-selected");
        }

        selectedCorporationCard = card;
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

                showWaitingForPlayer();
            }
            case PlayerType.CLIENT -> {
                GameClientThread client = ApplicationConfiguration.getInstance().getGameClient();

                if (client != null) {
                    client.sendCorporationChoice(selectedCorporation.name());
                }

                showWaitingForPlayer();
            }
            default -> {

                if (ApplicationConfiguration.getInstance().getPlayerType() == PlayerType.LOCAL) {
                    boolean allPlayersChoseCorporation = gameManager.getPlayers().stream()
                            .allMatch(p -> p.getCorporation() != null);

                    if (allPlayersChoseCorporation) {
                        ScreenNavigator.showInitialCardDraftScreen(gameManager);
                    } else {
                        ScreenNavigator.showChooseCorporationScreen(gameManager);
                    }
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