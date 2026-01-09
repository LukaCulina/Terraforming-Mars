package hr.terraforming.mars.terraformingmars.thread;

import hr.terraforming.mars.terraformingmars.model.GameMove;
import hr.terraforming.mars.terraformingmars.util.GameMoveUtils;
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

        Platform.runLater(() -> lastGameMove.ifPresent(lastMove -> GameMoveUtils.updateLastMoveLabel(label, lastMove)));
    }
}
