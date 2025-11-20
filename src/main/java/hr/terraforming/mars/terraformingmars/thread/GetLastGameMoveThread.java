package hr.terraforming.mars.terraformingmars.thread;

import hr.terraforming.mars.terraformingmars.model.GameMove;
import javafx.application.Platform;
import javafx.scene.control.Label;

import java.time.format.DateTimeFormatter;
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
                StringBuilder sb = new StringBuilder("Last Move: ");
                sb.append(move.playerName()).append(" - ");
                sb.append(move.actionType().toString());
                if (move.details() != null && !move.details().isEmpty()) {
                    sb.append(" (").append(move.details()).append(")");
                }
                if (move.row() != null) {
                    sb.append(" at [").append(move.row()).append(", ").append(move.col()).append("]");
                }
                sb.append(" at ").append(move.timestamp().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
                label.setText(sb.toString());
            }
        });
    }
}
