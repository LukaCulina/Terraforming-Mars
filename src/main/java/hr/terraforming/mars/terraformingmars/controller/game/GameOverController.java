package hr.terraforming.mars.terraformingmars.controller.game;

import hr.terraforming.mars.terraformingmars.model.Card;
import hr.terraforming.mars.terraformingmars.model.Player;
import hr.terraforming.mars.terraformingmars.ui.GameScreenResizer;
import javafx.scene.control.Label;
import javafx.fxml.FXML;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.util.Comparator;
import java.util.List;

public class GameOverController {

    @FXML private VBox gameOver;
    @FXML private Label winnerLabel;
    @FXML private GridPane scoresGrid;

    @FXML
    public void initialize() {
        GameScreenResizer.attachFontResizeListeners(gameOver, this::updateFontSizes);
    }

    private void updateFontSizes() {
        GameScreenResizer.updateFonts(
                gameOver,
                new GameScreenResizer.FontMapping(".main-title", 0.05),
                new GameScreenResizer.FontMapping(".choose-label", 0.035),
                new GameScreenResizer.FontMapping(".header-label", 0.03),
                new GameScreenResizer.FontMapping(".normal-label", 0.025)
        );
    }

    public void setFinalScores(List<Player> players) {
        players.sort(Comparator.comparingInt(Player::getFinalScore).reversed()
                .thenComparing(Player::getMC, Comparator.reverseOrder()));

        Player winner = players.getFirst();
        winnerLabel.setText("Winner: " + winner.getName() + " with " + winner.getFinalScore() + " victory points!");

        addGridHeader();
        for (int i = 0; i < players.size(); i++) {
            addPlayerRow(i + 1, players.get(i));
        }
    }

    private void addGridHeader() {
        int col = 0;
        scoresGrid.add(createHeaderLabel("Rank"), col++, 0);
        scoresGrid.add(createHeaderLabel("Player"), col++, 0);
        scoresGrid.add(createHeaderLabel("TR"), col++, 0);
        scoresGrid.add(createHeaderLabel("Milestones"), col++, 0);
        scoresGrid.add(createHeaderLabel("Board bonuses"), col++, 0);
        scoresGrid.add(createHeaderLabel("Cards"), col++, 0);
        scoresGrid.add(createHeaderLabel("Total"), col, 0);
    }

    private void addPlayerRow(int rank, Player player) {
        int rowIndex = scoresGrid.getRowCount();
        int col = 0;

        int cardPoints = player.getPlayed().stream().mapToInt(Card::getVictoryPoints).sum();

        scoresGrid.add(createNormalLabel(String.valueOf(rank)), col++, rowIndex);
        scoresGrid.add(createNormalLabel(player.getName()), col++, rowIndex);
        scoresGrid.add(createNormalLabel(String.valueOf(player.getTR())), col++, rowIndex);
        scoresGrid.add(createNormalLabel(String.valueOf(player.getMilestonePoints())), col++, rowIndex);
        scoresGrid.add(createNormalLabel(String.valueOf(player.getTilePoints())), col++, rowIndex);
        scoresGrid.add(createNormalLabel(String.valueOf(cardPoints)), col++, rowIndex);
        scoresGrid.add(createHeaderLabel(String.valueOf(player.getFinalScore())), col, rowIndex);
    }

    private Label createHeaderLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("header-label");
        return label;
    }

    private Label createNormalLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("normal-label");
        return label;
    }
}