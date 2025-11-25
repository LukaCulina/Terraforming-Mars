package hr.terraforming.mars.terraformingmars.util;

import hr.terraforming.mars.terraformingmars.config.ResourceConfig;
import hr.terraforming.mars.terraformingmars.exception.FxmlLoadException;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;
import lombok.extern.slf4j.Slf4j;
import java.io.IOException;
import java.util.function.Consumer;

@Slf4j
public class ScreenLoader {

    private ScreenLoader() {
        throw new IllegalStateException("Utility class");
    }

    private static ResourceConfig config;

    public record FxmlResult<T>(Parent root, T controller) {}

    private static final int LOADING_PANE_SIZE = 100;
    private static final int TRANSITION_DURATION = 200;

    public static void setConfig(ResourceConfig resourceConfig) {
        if (config != null) {
            throw new IllegalStateException("ScreenLoader configuration has already been set.");
        }
        config = resourceConfig;
    }

    private static void ensureConfigured() {
        if (config == null) {
            throw new IllegalStateException("ScreenLoader has not been configured. Please call setConfig() at startup.");
        }
    }

    public static <T> FxmlResult<T> loadFxml(String fxmlFile) {
        ensureConfigured();
        try {
            String fullFxmlPath = config.fxmlBasePath() + fxmlFile;
            FXMLLoader loader = new FXMLLoader(ScreenLoader.class.getResource(fullFxmlPath));
            Parent root = loader.load();
            T controller = loader.getController();
            return new FxmlResult<>(root, controller);
        } catch (IOException e) {
            throw new FxmlLoadException("Failed to load FXML: " + fxmlFile, e);
        }
    }

    public static Scene createScene(Parent root) {
        ensureConfigured();
        Scene scene = new Scene(root);

        var css = ScreenLoader.class.getResource(config.cssPath());

        if (css == null) {
            throw new IllegalStateException("CSS file not found: " + config.cssPath());
        }

        scene.getStylesheets().add(css.toExternalForm());
        return scene;
    }

    public static <T> void showAsMainScreen(Stage stage, String fxmlFile, String title, Consumer<T> controllerAction) {
        FxmlResult<T> result = loadFxml(fxmlFile);
        if (controllerAction != null) {
            controllerAction.accept(result.controller());
        }
        Scene scene = createScene(result.root());
        stage.setScene(scene);
        stage.setTitle(title);
    }

    public static <T> void showAsModal(
            Window owner,
            String fxmlFile,
            String title,
            double widthPercentage,
            double heightPercentage,
            Consumer<T> controllerAction) {

        Stage loadingStage = createLoadingStage(owner);
        PauseTransition delay = new PauseTransition(Duration.millis(TRANSITION_DURATION));
        delay.setOnFinished(_ -> loadingStage.show());

        Task<FxmlResult<T>> loadTask = new Task<>() {
            @Override
            protected FxmlResult<T> call() {
                return loadFxml(fxmlFile);
            }
        };

        loadTask.setOnSucceeded(_ -> {
            delay.stop();
            loadingStage.close();

            FxmlResult<T> result = loadTask.getValue();
            if (controllerAction != null) {
                controllerAction.accept(result.controller());
            }

            Stage stage = new Stage();
            stage.setTitle(title);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(owner);
            stage.setResizable(true);

            Scene scene = createScene(result.root());
            stage.setScene(scene);
            stage.setWidth(owner.getWidth() * widthPercentage);
            stage.setHeight(owner.getHeight() * heightPercentage);

            double centerX = owner.getX() + (owner.getWidth() - stage.getWidth()) / 2;
            double centerY = owner.getY() + (owner.getHeight() - stage.getHeight()) / 2;

            stage.setX(centerX);
            stage.setY(centerY);

            stage.showAndWait();
        });

        loadTask.setOnFailed(_ -> {
            delay.stop();
            loadingStage.close();
            log.error("Failed to load FXML file asynchronously: {}", fxmlFile, loadTask.getException());
            Platform.runLater(() -> new Alert(Alert.AlertType.ERROR, "Could not load screen: " + fxmlFile).showAndWait());
        });

        delay.play();
        new Thread(loadTask).start();
    }

    private static Stage createLoadingStage(Window owner) {
        ProgressIndicator progressIndicator = new ProgressIndicator();
        VBox loadingPane = new VBox(progressIndicator);
        loadingPane.setAlignment(Pos.CENTER);
        loadingPane.getStyleClass().add("loading-pane");

        Scene loadingScene = new Scene(loadingPane, LOADING_PANE_SIZE, LOADING_PANE_SIZE);
        loadingScene.setFill(null);

        Stage stage = new Stage();
        stage.setScene(loadingScene);
        stage.initOwner(owner);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setResizable(false);
        stage.setTitle("Loading...");

        return stage;
    }
}
