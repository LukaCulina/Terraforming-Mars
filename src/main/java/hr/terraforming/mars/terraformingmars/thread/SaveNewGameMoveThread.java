package hr.terraforming.mars.terraformingmars.thread;

import hr.terraforming.mars.terraformingmars.model.GameMove;

public class SaveNewGameMoveThread extends GameMoveThread implements Runnable {

    private final GameMove gameMove;

    public SaveNewGameMoveThread(GameMove newGameMove) {
        this.gameMove = newGameMove;
    }

    @Override
    public void run() {
        saveNewGameMoveToFile(gameMove);
    }
}
