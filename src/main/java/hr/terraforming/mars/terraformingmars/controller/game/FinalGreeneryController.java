package hr.terraforming.mars.terraformingmars.controller.game;

import hr.terraforming.mars.terraformingmars.enums.ResourceType;
import hr.terraforming.mars.terraformingmars.model.GameManager;
import hr.terraforming.mars.terraformingmars.model.Player;
import hr.terraforming.mars.terraformingmars.ui.ResizeHandler;
import hr.terraforming.mars.terraformingmars.view.ScreenNavigator;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class FinalGreeneryController {

    @FXML private VBox finalGreenery;
    @FXML private Label infoLabel;
    @FXML private Label playerNameLabel;
    @FXML private Label plantsLabel;
    @FXML private Label greeneryCostLabel;
    @FXML private Button convertButton;
    @FXML private Button finishButton;

    private GameManager gameManager;
    private List<Player> players;
    private int currentPlayerIndex = 0;
    private Player currentPlayer;
    private TerraformingMarsController mainController;
    private Runnable onComplete;
    private Stage stage;

    @FXML
    public void initialize() {
        ResizeHandler.attachFontResizeListeners(finalGreenery, this::updateFontSizes);
    }

    private void updateFontSizes() {
        ResizeHandler.updateFonts(
                finalGreenery,
                new ResizeHandler.FontMapping(".choose-label", 0.05),
                new ResizeHandler.FontMapping(".info-label", 0.025),
                new ResizeHandler.FontMapping(".player-name-label", 0.035),
                new ResizeHandler.FontMapping(".details-label", 0.025),
                new ResizeHandler.FontMapping(".confirm-button", 0.025),
                new ResizeHandler.FontMapping(".player-button", 0.025)
        );
    }

    public void setup(GameManager gameManager, TerraformingMarsController mainController) {
        this.players = gameManager.getPlayers();
        this.mainController = mainController;
        this.gameManager = gameManager;
        this.currentPlayerIndex = 0;
        Platform.runLater(() -> this.stage = (Stage) convertButton.getScene().getWindow());
        showCurrentPlayer();
    }

    public void setupSinglePlayer(Player player, GameManager gameManager,
                                  TerraformingMarsController mainController,
                                  Runnable onComplete) {
        this.currentPlayer = player;
        this.gameManager = gameManager;
        this.mainController = mainController;
        this.onComplete = onComplete;

        Platform.runLater(() -> {
            this.stage = (Stage) convertButton.getScene().getWindow();
            updateUI();
        });
    }

    private void showCurrentPlayer() {
        if (currentPlayerIndex >= players.size()) {
            onFinalGreeneryPhaseComplete();
            closeWindow();
            return;
        }

        this.currentPlayer = players.get(currentPlayerIndex);
        updateUI();
    }

    private void onFinalGreeneryPhaseComplete() {
        log.info("Final greenery conversion phase is complete. Proceeding to calculate final scores.");
        List<Player> rankedPlayers = gameManager.calculateFinalScores();
        Platform.runLater(() -> ScreenNavigator.showGameOverScreen(rankedPlayers));
    }

    private void updateUI() {
        if (currentPlayer == null) return;

        int plants = currentPlayer.resourceProperty(ResourceType.PLANTS).get();
        int cost = currentPlayer.getGreeneryCost();

        if (players != null) {
            infoLabel.setText("Player " + (currentPlayerIndex + 1) + "/" + players.size());
        } else {
            infoLabel.setText("Final Greenery Phase");
        }

        playerNameLabel.setText(currentPlayer.getName());
        plantsLabel.setText("🌿 Plants: " + plants);
        greeneryCostLabel.setText("(Cost: " + cost + ")");

        convertButton.setDisable(plants < cost);
    }

    @FXML
    private void handleConvertGreenery() {
        if (currentPlayer == null) return;

        mainController.getPlacementManager().enterPlacementModeForFinalGreenery(
                currentPlayer,
                () -> {
                    if (this.stage != null) {
                        this.stage.show();
                        updateUI();
                    }
                }
        );

        if (this.stage != null) {
            this.stage.hide();
        }
    }

    @FXML
    private void handleFinish() {
        log.info("{} has finished their greenery conversion.", currentPlayer.getName());

        if (onComplete != null) {
            closeWindow();
            onComplete.run();
            return;
        }

        currentPlayerIndex++;
        showCurrentPlayer();
    }

    private void closeWindow() {
        if (stage != null) {
            stage.close();
        } else if (playerNameLabel != null && playerNameLabel.getScene() != null) {
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
