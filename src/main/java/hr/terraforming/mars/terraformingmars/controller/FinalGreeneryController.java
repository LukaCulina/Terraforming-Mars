package hr.terraforming.mars.terraformingmars.controller;

import hr.terraforming.mars.terraformingmars.model.Player;
import hr.terraforming.mars.terraformingmars.enums.ResourceType;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class FinalGreeneryController {

    private static final Logger logger = LoggerFactory.getLogger(FinalGreeneryController.class);

    @FXML
    private Label infoLabel;
    @FXML
    private Label playerNameLabel;
    @FXML
    private Label plantsLabel;
    @FXML
    private Label greeneryCostLabel;
    @FXML
    private Button convertButton;

    private List<Player> players;
    private int currentPlayerIndex = 0;
    private TerraformingMarsController mainController;
    private Stage stage;

    public void setup(List<Player> players, TerraformingMarsController mainController) {
        this.players = players;
        this.mainController = mainController;
        this.currentPlayerIndex = 0;
        Platform.runLater(() -> this.stage = (Stage) convertButton.getScene().getWindow());
        showCurrentPlayer();
    }

    private void showCurrentPlayer() {
        if (currentPlayerIndex >= players.size()) {
            mainController.onFinalGreeneryPhaseComplete();
            closeWindow();
            return;
        }

        Player currentPlayer = players.get(currentPlayerIndex);
        infoLabel.setText("Player " + (currentPlayerIndex + 1) + "/" + players.size());
        playerNameLabel.setText(currentPlayer.getName());
        updateUI();
    }

    private void updateUI() {
        Player currentPlayer = players.get(currentPlayerIndex);
        int plants = currentPlayer.resourceProperty(ResourceType.PLANTS).get();
        int cost = currentPlayer.getGreeneryCost();

        plantsLabel.setText("🌿 Plants: " + plants);
        greeneryCostLabel.setText("(Cost: " + cost + ")");

        convertButton.setDisable(plants < cost);
    }

    @FXML
    private void handleConvertGreenery() {
        Player currentPlayer = players.get(currentPlayerIndex);

        mainController.enterPlacementModeForFinalGreenery(currentPlayer, () -> {
            if (this.stage != null) {
                this.stage.show();
                updateUI();
            }
        });

        if (this.stage != null) {
            this.stage.hide();
        }
    }

    @FXML
    private void handleFinish() {
        logger.info("{} has finished their greenery conversion.", players.get(currentPlayerIndex).getName());

        currentPlayerIndex++;
        showCurrentPlayer();
    }

    private void closeWindow() {
        stage.close();
    }
}
