package hr.terraforming.mars.terraformingmars.ui;

import hr.terraforming.mars.terraformingmars.controller.PlayerBoardController;
import hr.terraforming.mars.terraformingmars.exception.FxmlLoadException;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;
import java.io.IOException;

@Slf4j
public class PlayerBoardLoader {

    private PlayerBoardLoader() {
        throw new IllegalStateException("Utility class");
    }

    public static PlayerBoardController loadPlayerBoard(VBox container) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    PlayerBoardLoader.class.getResource("/hr/terraforming/mars/terraformingmars/PlayerBoard.fxml")
            );
            Node boardNode = loader.load();
            PlayerBoardController controller = loader.getController();

            container.getChildren().setAll(boardNode);

            log.info("PlayerBoard successfully loaded.");
            return controller;

        } catch (IOException e) {
            log.error("FATAL: Could not load PlayerBoard.fxml. The application cannot start correctly.", e);
            throw new FxmlLoadException("Failed to load PlayerBoard.fxml", e);
        }
    }
}
