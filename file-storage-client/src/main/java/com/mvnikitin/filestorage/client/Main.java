package com.mvnikitin.filestorage.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception{
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(
                PropertiesController.class.getResource("/logon.fxml"));
        VBox page = loader.load();
        primaryStage.setTitle("Net Memory");
        primaryStage.setScene(new Scene(page, 1200, 800));

        LogonController controller = loader.getController();
        controller.setStage(primaryStage);

        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}