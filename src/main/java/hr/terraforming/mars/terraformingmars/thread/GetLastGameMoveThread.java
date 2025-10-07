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
                sb.append(lastGameMove.playerName()).append(" - ");
                sb.append(lastGameMove.actionDescription());

                if (lastGameMove.row() != null && lastGameMove.col() != null) {
                    sb.append(" at coordinates (").append(lastGameMove.row()).append(", ").append(lastGameMove.col()).append(")");
                }

                sb.append(" at ").append(lastGameMove.timestamp().format(DateTimeFormatter.ofPattern("HH:mm:ss")));

                label.setText(sb.toString());
            } else {
                label.setText("No moves recorded yet.");
            }
        });
    }
}
