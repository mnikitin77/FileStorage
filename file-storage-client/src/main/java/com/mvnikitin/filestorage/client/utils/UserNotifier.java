package com.mvnikitin.filestorage.client.utils;

import javafx.scene.control.Alert;
import javafx.stage.Window;

public class UserNotifier {
    public static void showInfoMessage(String title,
                                       String header,
                                       String content,
                                       Window owner) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        showAlert(alert, title, header, content, owner);
    }

    public static void showErrorMessage(String title,
                                        String header,
                                        String content,
                                        Window owner) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        showAlert(alert, title, header, content, owner);
    }

    private static void showAlert(Alert alert, String title, String header, String content, Window owner) {
        if (owner != null) {
            alert.initOwner(owner);
        }
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
