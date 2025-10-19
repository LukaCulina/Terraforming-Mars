package hr.terraforming.mars.terraformingmars.util;

import hr.terraforming.mars.terraformingmars.model.GameMove;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class GameMoveUtils {

    private GameMoveUtils() {
        throw new IllegalStateException("Utility class");
    }

    private static final Logger logger = LoggerFactory.getLogger(GameMoveUtils.class);

    private static final String GAME_MOVE_HISTORY_FILE_NAME = "gameMoves/moves.dat";

    public static void saveNewGameMove(GameMove newGameMove) {
        List<GameMove> gameMoveList = loadAllGameMoves();
        gameMoveList.add(newGameMove);

        File file = new File(GAME_MOVE_HISTORY_FILE_NAME);
        File parentDir = file.getParentFile();

        if (parentDir != null && !parentDir.exists() && !parentDir.mkdirs()) {
                logger.error("Failed to create directory: {}", parentDir.getAbsolutePath());
                return;
            }

        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
            oos.writeObject(gameMoveList);
        } catch (IOException e) {
            logger.error("Failed to save game moves to file '{}'", GAME_MOVE_HISTORY_FILE_NAME, e);
        }
    }

    public static GameMove getLastGameMove() {
        List<GameMove> gameMoveList = loadAllGameMoves();
        if (gameMoveList.isEmpty()) {
            return null;
        }
        return gameMoveList.getLast();
    }

    @SuppressWarnings("unchecked")
    private static List<GameMove> loadAllGameMoves() {
        File file = new File(GAME_MOVE_HISTORY_FILE_NAME);
        if (!file.exists()) {
            return new ArrayList<>();
        }

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            return (List<GameMove>) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            logger.error("Error loading game moves: '{}'", e.getMessage());
            return new ArrayList<>();
        }
    }

    public static void deleteMoveHistoryFile() {
        File file = new File(GAME_MOVE_HISTORY_FILE_NAME);
        if (file.exists()) {
            try {
                Files.delete(file.toPath());
                logger.info("Game move history file deleted.");
            } catch (IOException e) {
                logger.error("Failed to delete game move history file.", e);
            }
        }
    }
}
