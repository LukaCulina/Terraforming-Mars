package hr.terraforming.mars.terraformingmars.util;

import hr.terraforming.mars.terraformingmars.model.GameMove;
import hr.terraforming.mars.terraformingmars.thread.GetLastGameMoveThread;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.control.Label;
import javafx.util.Duration;
import lombok.extern.slf4j.Slf4j;
import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
public class GameMoveUtils {

    private GameMoveUtils() {
        throw new IllegalStateException("Utility class");
    }

    private static final String GAME_MOVE_HISTORY_FILE_NAME = "gameMoves/moves.dat";

    public static void saveNewGameMove(GameMove newGameMove) {
        List<GameMove> gameMoveList = loadAllGameMoves();
        gameMoveList.add(newGameMove);

        File file = new File(GAME_MOVE_HISTORY_FILE_NAME);
        File parentDir = file.getParentFile();

        if (parentDir != null && !parentDir.exists() && !parentDir.mkdirs()) {
                log.error("Failed to create directory: {}", parentDir.getAbsolutePath());
                return;
            }

        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
            oos.writeObject(gameMoveList);
        } catch (IOException e) {
            log.error("Failed to save game moves to file '{}'", GAME_MOVE_HISTORY_FILE_NAME, e);
        }
    }

    public static Optional<GameMove> getLastGameMove() {
        List<GameMove> gameMoveList = loadAllGameMoves();
        if (gameMoveList.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(gameMoveList.getLast());
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
            log.error("Error loading game moves: '{}'", e.getMessage());
            return new ArrayList<>();
        }
    }

    public static void deleteMoveHistoryFile() {
        File file = new File(GAME_MOVE_HISTORY_FILE_NAME);
        if (file.exists()) {
            try {
                Files.delete(file.toPath());
                log.info("Game move history file deleted.");
            } catch (IOException e) {
                log.error("Failed to delete game move history file.", e);
            }
        }
    }

    public static Timeline createLastMoveTimeline(Label lastMoveLabel) {
        Timeline theLastMoveTimeline = new Timeline(new KeyFrame(Duration.seconds(3), _ -> {
            GetLastGameMoveThread thread = new GetLastGameMoveThread(lastMoveLabel);
            Thread t = new Thread(thread);
            t.setDaemon(true);
            t.start();
        }));
        theLastMoveTimeline.setCycleCount(Animation.INDEFINITE);
        return theLastMoveTimeline;
    }
}
