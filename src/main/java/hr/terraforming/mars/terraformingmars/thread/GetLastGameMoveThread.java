package hr.terraforming.mars.terraformingmars.thread;

import hr.terraforming.mars.terraformingmars.model.GameMove;
import javafx.application.Platform;
import javafx.scene.control.Label;

import java.time.format.DateTimeFormatter;

public class GetLastGameMoveThread extends GameMoveThread implements Runnable {
    private final Label label;

    public GetLastGameMoveThread(Label label) {
        this.label = label;
    }

    @Override
    public void run() {
        GameMove lastGameMove = getLastGameMoveFromFile();

        Platform.runLater(() -> {
            if (lastGameMove != null) {
                StringBuilder sb = new StringBuilder("Last Move: ");
                sb.append(lastGameMove.getPlayerName()).append(" - ");
                sb.append(lastGameMove.getActionType().toString());
                if (lastGameMove.getDetails() != null && !lastGameMove.getDetails().isEmpty()) {
                    sb.append(" (").append(lastGameMove.getDetails()).append(")");
                }
                if (lastGameMove.getRow() != null) {
                    sb.append(" at [").append(lastGameMove.getRow()).append(", ").append(lastGameMove.getCol()).append("]");
                }
                sb.append(" at ").append(lastGameMove.getTimestamp().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
                label.setText(sb.toString());
            } else {
                label.setText("No moves recorded yet.");
            }
        });
    }
}
