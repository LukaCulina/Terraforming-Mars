package hr.terraforming.mars.terraformingmars.thread;

import hr.terraforming.mars.terraformingmars.model.GameMove;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SaveNewGameMoveThread extends GameMoveThread implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(SaveNewGameMoveThread.class);

    private final GameMove gameMove;

    public SaveNewGameMoveThread(GameMove newGameMove) {
        this.gameMove = newGameMove;
    }

    @Override
    public void run() {
        saveNewGameMoveToFile(gameMove);
        logger.info("Move saved: '{}'", gameMove.actionDescription());
    }
}
