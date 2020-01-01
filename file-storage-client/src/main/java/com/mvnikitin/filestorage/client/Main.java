package com.mvnikitin.filestorage.client;

import com.mvnikitin.filestorage.client.utils.NetworkManager;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception{
        //Parent root = FXMLLoader.load(getClass().getResource("/main.fxml"));
        Parent root = FXMLLoader.load(getClass().getResource("/logon.fxml"));
        primaryStage.setTitle("Net Memory");
        primaryStage.setScene(new Scene(root, 1200, 800));

        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}