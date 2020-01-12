package com.mvnikitin.filestorage.client;

import com.mvnikitin.filestorage.client.utils.ClientProperties;
import com.mvnikitin.filestorage.client.utils.UserNotifier;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class PropertiesController implements Initializable {

    @FXML
    private TextField propHostText, propPortText, propUserText;

    private Stage dialogStage;

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        propHostText.setText(ClientProperties.getInstance().getHost());
        propPortText.setText(
                Integer.toString(ClientProperties.getInstance().getPort()));
        propUserText.setText(ClientProperties.getInstance().getUsername());
    }

    @FXML
    public void handleOk(ActionEvent actionEvent) {
        if (isDataValid()) {
            ClientProperties.getInstance().setHost(propHostText.getText());
            ClientProperties.getInstance().setPort(
                    Integer.parseInt(propPortText.getText()));
            ClientProperties.getInstance().setUsername(propUserText.getText());
            try {
                ClientProperties.getInstance().saveProperties();
            } catch (IOException e) {
                UserNotifier.showErrorMessage("Error",
                        "Error when saving settings.",
                        e.getMessage(),
                        dialogStage);
            }

            dialogStage.close();
        }
    }

    @FXML
    public void handleCancel(ActionEvent actionEvent) {
        dialogStage.close();
    }

    private boolean isDataValid() {

        if (propPortText.getText().matches("\\d+")) {
            try {
                int port = Integer.parseInt(propPortText.getText());
                if (port >= 1024 || port <= 65535) {
                    return true;
                }
            } catch (NumberFormatException e) {

            }
        }

        UserNotifier.showErrorMessage("Error",
                "Invalid value.",
                "[Port] parameter must be within 1024 and 65535.",
                dialogStage);

        return false;
    }
}
