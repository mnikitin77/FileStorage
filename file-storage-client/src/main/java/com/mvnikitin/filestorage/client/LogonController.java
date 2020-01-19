package com.mvnikitin.filestorage.client;

import com.mvnikitin.filestorage.client.utils.ClientProperties;
import com.mvnikitin.filestorage.client.utils.NetworkManager;
import com.mvnikitin.filestorage.client.utils.ServerCommandSender;
import com.mvnikitin.filestorage.client.utils.UserNotifier;
import com.mvnikitin.filestorage.common.message.AbstractNetworkMessage;
import com.mvnikitin.filestorage.common.message.service.LogonCommand;
import com.mvnikitin.filestorage.common.message.service.RegisterCommand;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class LogonController implements Initializable {
    private String username;
    private ServerCommandSender serverCommandSender;

    private Stage stage;

    @FXML
    private VBox logonPanel, registerPanel;
    @FXML
    private TextField usernameText, passwordPwd,
            regUserText, regPwd1Text, regPwd2Text;
    @FXML
    private Button logonBtn, registerBtn;
    @FXML
    private MenuBar menuBar;

    public void setStage(Stage stage) {
        this.stage = stage;
        serverCommandSender.setOwner(stage);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        serverCommandSender = new ServerCommandSender();

        try {
            NetworkManager.start(
                    ClientProperties.getInstance().getHost(),
                    ClientProperties.getInstance().getPort()
            );

            username = ClientProperties.getInstance().getUsername();
            if(username.length() != 0) {
                usernameText.setText(username);

                Platform.runLater(()-> passwordPwd.requestFocus());
            }

            logonPanel.getParent().setOnKeyPressed(event -> {
                if (event.getCode() == KeyCode.ENTER) {
                    if (logonPanel.isVisible()) {
                        logonBtn.fire();
                    } else {
                        registerBtn.fire();
                    }
                }
            });

            showLogonPanel();

        } catch (IOException e) {
            UserNotifier.showErrorMessage("Network error",
                    "Unable to connect to the server.",
                    e.getMessage(),
                    null);

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

    @FXML
    public void showProperties(ActionEvent actionEvent) {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(
                    PropertiesController.class.getResource("/properties.fxml"));
            VBox root = loader.load();
            Stage dialogStage = new Stage();
            dialogStage.setTitle("Edit settings");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(menuBar.getScene().getWindow());
            dialogStage.setScene(new Scene(root));

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
        UserNotifier.showInfoMessage("About",
                "Net Memory application.",
                "Maxim Nikitin 2019",
                null);
    }

    @FXML
    public void logon(ActionEvent actionEvent) {
        String username = usernameText.getText();
        int password = passwordPwd.getText().hashCode();

        AbstractNetworkMessage msg = new LogonCommand(username, password);
        msg = serverCommandSender.sendMessage(msg);
        if (msg != null) {
            // Switch to the application main screen
            try {
                FXMLLoader loader = new FXMLLoader();
                loader.setLocation(
                        PropertiesController.class.getResource("/main.fxml"));
                VBox root = loader.load();
                Scene mainScene = new Scene(root);
                Stage stage = (Stage) ((Node) actionEvent
                        .getSource()).getScene().getWindow();
                stage.hide();
                stage.setScene(mainScene);

                Controller controller = loader.getController();
                controller.setStage(stage);
                stage.show();

            // Saving the username in the properties file.
                ClientProperties.getInstance().setUsername(username);
                ClientProperties.getInstance().saveProperties();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    public void register(ActionEvent actionEvent) {
        String username = regUserText.getText();
        String password1 = regPwd1Text.getText();
        String password2 = regPwd2Text.getText();

        if (checkRegForm(username, password1, password2)) {
            AbstractNetworkMessage msg =
                    new RegisterCommand(username, password1.hashCode());
            msg = serverCommandSender.sendMessage(msg);
            if (msg != null) {
             // Switch to the logon panel.
                usernameText.setText(username);
                showLogonPanel();
            }
        }
    }

    private boolean checkRegForm (String username, String pwd1, String pwd2) {
        String messageText;

        if (username.isEmpty())
            messageText = "Please fill in the Username.";
        else if (pwd1.isEmpty())
            messageText = "Please fill in the Password.";
        else if (pwd2.isEmpty())
            messageText = "Please repeat the Password.";
        else if (!pwd1.equals(pwd2))
            messageText = "Please fill in the same password two times.";
        else if (!checkPasswordStrengh(pwd2)) {
            messageText = "The password is weak. " +
                    "A password must contain at least one capital [A-Z], " +
                    "one digit [0-9], one small letter [a-z], " +
                    "one special symbol [@#$%^&+=]. " +
                    "It also must have at least 8 symbols but not more that 20.";
        } else {
            return true;
        }

        UserNotifier.showInfoMessage("Information",
                            "Unable to register a user.",
                            messageText,
                            null);

        return false;
    }

    private boolean checkPasswordStrengh(String s){
        Pattern p = Pattern.compile(
                "(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])" +
                        "(?=.*[@#$%^&+=])(?=\\S+$).{8,20}");
        Matcher m = p.matcher(s);
        if(m.matches()){
            return true;
        }
        return false;
    }
}