package com.mvnikitin.filestorage.client;

import com.mvnikitin.filestorage.client.utils.ClientProperties;
import com.mvnikitin.filestorage.client.utils.NetworkManager;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;


public class LogonController implements Initializable {
    private String username;

    @FXML
    private VBox logonPanel, registerPanel;
    @FXML
    private TextField usernameText, passwordPwd;
    @FXML
    private MenuBar menuBar;

    private boolean isAuthorized;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            NetworkManager.start(
                    ClientProperties.getInstance().getHost(),
                    ClientProperties.getInstance().getPort()
            );

            username = ClientProperties.getInstance().getUsername();
            if(username.length() != 0) {
                usernameText.setText(username);

                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        passwordPwd.requestFocus();
                    }
                });
            }

            showLogonPanel();

        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Network error");
            alert.setHeaderText("Unable to connect to the server.");
            alert.setContentText(e.getMessage());
            alert.showAndWait();

            System.exit(-1);
        }
    }

    @FXML
    public void showLogonPanel() {
        logonPanel.setVisible(true);
        logonPanel.setManaged(true);
        registerPanel.setVisible(false);
        registerPanel.setManaged(false);
    }

    @FXML
    public void showRegsiterPanel() {
        registerPanel.setVisible(true);
        registerPanel.setManaged(true);
        logonPanel.setVisible(false);
        logonPanel.setManaged(false);
    }

    private void setAuthorized(boolean isAuthorized, Stage stage) throws IOException {
        this.isAuthorized = isAuthorized;
        if (isAuthorized) {
            // TODO: change the scene
//            Parent root =  FXMLLoader.load(getClass().getResource("/main.fxml"));
//            Scene mainScene = new Scene(root);
//            // в методе - обраюотчике события получить Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
//            stage.hide();
//            stage.setScene(mainScene);
//            stage.show();
        } else {
            // TODO:change the scene to Logon
        }
    }

    @FXML
    public void showProperties(ActionEvent actionEvent) {
        try {
            FXMLLoader loader = new FXMLLoader();
            //Parent root = loader.load(getClass().getResource("/properties.fxml"));
            loader.setLocation(PropertiesController.class.getResource("/properties.fxml"));
            VBox page = (VBox) loader.load();
            Stage dialogStage = new Stage();
            dialogStage.setTitle("Edit settings");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(menuBar.getScene().getWindow());
            dialogStage.setScene(new Scene(page));

            PropertiesController controller = loader.getController();
            controller.setDialogStage(dialogStage);
            dialogStage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @FXML
    public void exitApplication(ActionEvent actionEvent) {
        NetworkManager.stop();
        Platform.exit();
    }

    @FXML
    public void showAbout(ActionEvent actionEvent) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText("Net Memory application.");
        alert.setContentText("Maxim Nikitin 2019");
        alert.show();
    }
}