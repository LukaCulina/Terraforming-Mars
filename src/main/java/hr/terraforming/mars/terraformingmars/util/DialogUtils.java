package hr.terraforming.mars.terraformingmars.util;

import javafx.scene.control.Alert;

public class DialogUtils {

    private DialogUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static void showDialog(Alert.AlertType alertType,
                                  String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
