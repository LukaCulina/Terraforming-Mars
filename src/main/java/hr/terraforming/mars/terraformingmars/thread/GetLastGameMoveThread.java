package hr.terraforming.mars.terraformingmars.thread;

import hr.terraforming.mars.terraformingmars.model.GameMove;
import javafx.application.Platform;
import javafx.scene.control.Label;

import java.util.Optional;

public class GetLastGameMoveThread extends GameMoveThread implements Runnable {

    private final Label label;

    public GetLastGameMoveThread(Label label) {
        this.label = label;
    }

    @Override
    public void run() {
        Optional<GameMove> lastGameMove = getLastGameMoveFromFile();

        Platform.runLater(() -> {
            if (lastGameMove.isPresent()) {
                GameMove move = lastGameMove.get();
                StringBuilder sb = new StringBuilder();
                sb.append(move.playerName()).append(" ");
                if (move.message() != null && !move.message().isEmpty()) {
                    sb.append(move.message());
                }
                if (move.row() != null) {
                    sb.append(" at [").append(move.row()).append(", ").append(move.col()).append("]");
                }
                label.setText(sb.toString());
            }
        });
    }
}
