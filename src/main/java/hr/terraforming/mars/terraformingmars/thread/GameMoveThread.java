package hr.terraforming.mars.terraformingmars.thread;

import hr.terraforming.mars.terraformingmars.model.GameMove;
import hr.terraforming.mars.terraformingmars.util.GameMoveUtils;

public abstract class GameMoveThread implements Runnable {

    public static final Object FILE_LOCK = new Object();

    protected void saveNewGameMoveToFile(GameMove gameMove) {
        synchronized (FILE_LOCK) {
            GameMoveUtils.saveNewGameMove(gameMove);
        }
    }

    protected GameMove getLastGameMoveFromFile() {
        synchronized (FILE_LOCK) {
            return GameMoveUtils.getLastGameMove();
        }
    }
}