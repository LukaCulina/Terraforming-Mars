package hr.terraforming.mars.terraformingmars.thread;

import hr.terraforming.mars.terraformingmars.model.GameMove;

public class SaveNewGameMoveThread extends GameMoveThread implements Runnable {

    private final GameMove gameMove;

    public SaveNewGameMoveThread(GameMove gameMove) {
        this.gameMove = gameMove;
    }

    @Override
    public void run() {
        saveNewGameMoveToFile(gameMove);
    }
}
