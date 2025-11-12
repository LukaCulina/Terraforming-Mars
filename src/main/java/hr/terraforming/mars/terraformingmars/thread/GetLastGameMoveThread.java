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
                sb.append(move.getPlayerName()).append(" - ");
                sb.append(move.getActionType().toString());
                if (move.getDetails() != null && !move.getDetails().isEmpty()) {
                    sb.append(" (").append(move.getDetails()).append(")");
                }
                if (move.getRow() != null) {
                    sb.append(" at [").append(move.getRow()).append(", ").append(move.getCol()).append("]");
                }
                sb.append(" at ").append(move.getTimestamp().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
                label.setText(sb.toString());
            }
        });
    }
}
