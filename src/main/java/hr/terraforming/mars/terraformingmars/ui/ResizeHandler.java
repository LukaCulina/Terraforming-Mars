package hr.terraforming.mars.terraformingmars.ui;

import hr.terraforming.mars.terraformingmars.view.HexBoardDrawer;
import javafx.scene.layout.AnchorPane;

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
}

