package hr.terraforming.mars.terraformingmars.controller;

import hr.terraforming.mars.terraformingmars.enums.ActionType;
import hr.terraforming.mars.terraformingmars.model.GameManager;
import hr.terraforming.mars.terraformingmars.model.GameMove;
import hr.terraforming.mars.terraformingmars.model.Player;
import hr.terraforming.mars.terraformingmars.enums.ResourceType;
import hr.terraforming.mars.terraformingmars.view.GameScreens;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import javafx.util.Duration;
import lombok.extern.slf4j.Slf4j;
import java.util.List;

@Slf4j
public class FinalGreeneryController {

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
    @FXML
    private Button finishButton;
    private GameManager gameManager;
    private List<Player> players;
    private int currentPlayerIndex = 0;
    private TerraformingMarsController mainController;
    private Stage stage;

    public void setup(GameManager gameManager, TerraformingMarsController mainController) {
        this.players = gameManager.getPlayers();
        this.mainController = mainController;
        this.gameManager = gameManager;
        this.currentPlayerIndex = 0;
        Platform.runLater(() -> this.stage = (Stage) convertButton.getScene().getWindow());
        showCurrentPlayer();
    }

    public void onFinalGreeneryPhaseComplete() {
        log.info("Final greenery conversion phase is complete. Proceeding to calculate final scores.");
        List<Player> rankedPlayers = gameManager.calculateFinalScores();

        Platform.runLater(() -> GameScreens.showGameOverScreen(rankedPlayers));
    }

    private void showCurrentPlayer() {
        if (currentPlayerIndex >= players.size()) {
            onFinalGreeneryPhaseComplete();
            closeWindow();
            return;
        }

        Player currentPlayer = players.get(currentPlayerIndex);

        String details = currentPlayer.getName() + "," + currentPlayer.resourceProperty(ResourceType.PLANTS).get() + "," + currentPlayer.getGreeneryCost();
        GameMove showModal = new GameMove(
                "System",
                ActionType.OPEN_FINAL_GREENERY_MODAL,
                details,
                java.time.LocalDateTime.now()
        );
        mainController.getActionManager().recordAndSaveMove(showModal);

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
        log.info("{} has finished their greenery conversion.", players.get(currentPlayerIndex).getName());

        currentPlayerIndex++;
        showCurrentPlayer();
    }

    private void closeWindow() {
        if (playerNameLabel != null && playerNameLabel.getScene() != null) {
            Stage windowToClose = (Stage) playerNameLabel.getScene().getWindow();
            if (windowToClose != null) {
                windowToClose.close();
            }
        }
    }

    public void replayShowFinalGreenery(String playerName, int plants, int cost) {
        infoLabel.setText("Replay: Final Greenery Conversion");
        playerNameLabel.setText(playerName);
        plantsLabel.setText("Plants: " + plants);
        greeneryCostLabel.setText("Cost: " + cost);

        convertButton.setVisible(false);
        finishButton.setVisible(false);

        PauseTransition autoClose = new PauseTransition(Duration.seconds(2));
        autoClose.setOnFinished(_ -> closeWindow());
        autoClose.play();
    }
}
