package hr.terraforming.mars.terraformingmars.ui;

import hr.terraforming.mars.terraformingmars.view.HexBoardDrawer;
import javafx.animation.PauseTransition;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.util.Duration;

public class ResizeHandler {

    private ResizeHandler() {
        throw new IllegalStateException("Utility class");
    }

    public static void attachResizeListeners(AnchorPane pane, HexBoardDrawer drawer) {
        PauseTransition resizePause = new PauseTransition(Duration.millis(100));

        Runnable triggerRedraw = () -> {
            resizePause.stop();
            resizePause.setOnFinished(_ -> drawer.drawBoard());
            resizePause.playFromStart();
        };

        pane.widthProperty().addListener((_, oldV, newV) -> {
            if (Math.abs(newV.doubleValue() - oldV.doubleValue()) > 10) triggerRedraw.run();
        });

        pane.heightProperty().addListener((_, oldV, newV) -> {
            if (Math.abs(newV.doubleValue() - oldV.doubleValue()) > 10) triggerRedraw.run();
        });
    }

    public static void attachFontResizeListeners(Pane pane, Runnable callback) {
        PauseTransition resizePause = new PauseTransition(Duration.millis(100));

        Runnable scheduleCallback = () -> {
            resizePause.stop();
            resizePause.setOnFinished(_ -> callback.run());
            resizePause.playFromStart();
        };

        pane.widthProperty().addListener((_, _, _) -> scheduleCallback.run());
        pane.heightProperty().addListener((_, _, _) -> scheduleCallback.run());
    }

    public static void updateFonts(Pane pane, FontMapping... mappings) {
        if (pane.getWidth() == 0 || pane.getHeight() == 0) return;

        double base = Math.min(pane.getWidth(), pane.getHeight());

        for (FontMapping mapping : mappings) {
            double fontSize = base * mapping.scaleFactor;
            String style = String.format("-fx-font-size: %.2fpx;", fontSize);

            pane.lookupAll(mapping.styleClass).forEach(node -> {
                if (node instanceof javafx.scene.text.Text || node instanceof Label) {
                    (node).setStyle(style);
                }
            });
        }
    }

    public record FontMapping(String styleClass, double scaleFactor) {}
}

