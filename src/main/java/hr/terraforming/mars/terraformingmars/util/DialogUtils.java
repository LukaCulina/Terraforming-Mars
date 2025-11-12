package hr.terraforming.mars.terraformingmars.util;

import javafx.scene.control.Alert;

public class DialogUtils {

    private DialogUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static void showSuccessDialog(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("The action was successful!");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
