package hr.terraforming.mars.terraformingmars.ui;

import hr.terraforming.mars.terraformingmars.view.HexBoardDrawer;
import javafx.animation.PauseTransition;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.util.Duration;

public class ResizeHandler {

    private ResizeHandler() {
        throw new IllegalStateException("Utility class");
    }

    public static void attachResizeListeners(AnchorPane pane, HexBoardDrawer drawer) {
        addWidthHeightListener(pane, drawer);
    }

    public static void addWidthHeightListener(AnchorPane pane, HexBoardDrawer drawer) {
        pane.widthProperty().addListener((_, oldV, newV) -> {
            if (Math.abs(newV.doubleValue() - oldV.doubleValue()) > 10) drawer.drawBoard();
        });

        pane.heightProperty().addListener((_, oldV, newV) -> {
            if (Math.abs(newV.doubleValue() - oldV.doubleValue()) > 10) drawer.drawBoard();
        });
    }

    public static void attachFontResizeListeners(Pane pane, Runnable callback) {
        PauseTransition resizePause = new PauseTransition(Duration.millis(150));

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
            pane.lookupAll(mapping.styleClass).forEach(node ->
                    node.setStyle(String.format("-fx-font-size: %.2fpx;", fontSize))
            );
        }
    }

    public record FontMapping(String styleClass, double scaleFactor) {}
}

