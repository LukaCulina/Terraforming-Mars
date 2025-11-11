package hr.terraforming.mars.terraformingmars.service;

import hr.terraforming.mars.terraformingmars.model.GameBoard;
import hr.terraforming.mars.terraformingmars.model.GameManager;
import hr.terraforming.mars.terraformingmars.model.GameState;
import hr.terraforming.mars.terraformingmars.util.DialogUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

public class GameSaveLoadService {

    private static final Logger logger = LoggerFactory.getLogger(GameSaveLoadService.class);
    public static final String SAVE_GAME_FILE_NAME = "saveGame/gameSave.dat";

    public GameSaveLoadService() {
        // No-op
    }

    public void saveGame(GameManager gameManager, GameBoard gameBoard) {
        GameState gameState = new GameState(gameManager, gameBoard);

        File file = new File(SAVE_GAME_FILE_NAME);
        File parentDir = file.getParentFile();
        if (!parentDir.exists()) {
            if (parentDir.mkdirs()) {
                logger.info("Created directory: {}", parentDir.getAbsolutePath());
            } else {
                logger.error("Failed to create directory: {}", parentDir.getAbsolutePath());
                return;
            }
        }

        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
            oos.writeObject(gameState);
            DialogUtils.showSuccessDialog("The game has been successfully saved!");
            logger.info("Game state saved to {}", file.getAbsolutePath());
        } catch (IOException e) {
            logger.error("Failed to save game state to {}", file.getAbsolutePath(), e);
        }
    }

    public GameState loadGame() {
        File file = new File(SAVE_GAME_FILE_NAME);

        if (file.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                GameState loadedState = (GameState) ois.readObject();
                logger.info("Game state loaded from {}", file.getAbsolutePath());
                return loadedState;
            } catch (IOException | ClassNotFoundException e) {
                logger.error("Failed to load game state from {}", file.getAbsolutePath(), e);
            }
        } else {
            logger.warn("Save file not found at: {}", file.getAbsolutePath());
        }
        return null;
    }
}
