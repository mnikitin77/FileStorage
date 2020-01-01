package com.mvnikitin.filestorage.client.utils;

import javafx.scene.control.Alert;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Properties;

public class ClientProperties {
    private int port;
    private String host;
    private String username;

    private static ClientProperties instance;

    public static ClientProperties getInstance() {
        if (instance == null) {
            try {
                instance = new ClientProperties();
                instance.init();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return instance;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
//        properties
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    private boolean isPropertiesExist() {
        return new File("filestorage.properties").exists();
    }

    public void saveProperties() throws IOException {
        try (FileWriter writer = new FileWriter("filestorage.properties")) {
            Properties properties = new Properties();
            properties.put("port", Integer.toString(port));
            properties.put("host", host);
            properties.put("username", username);
            properties.store(writer, null);
        }
    }

    private void createDefaultProperties() throws IOException {
        try (FileWriter writer = new FileWriter("filestorage.properties")){
            Properties properties = new Properties();
            properties.put("port", "8189");
            properties.put("host", "localhost");
            properties.put("username", "");
            properties.store(writer, null);
        }
    }

    private void init() throws IOException {
        if (!isPropertiesExist()) {
            createDefaultProperties();
        }

        try (FileReader reader = new FileReader("filestorage.properties")) {
            Properties properties = new Properties();
            properties.load(reader);
            port = Integer.parseInt(properties.getProperty("port"));
            host = properties.getProperty("host");
            username = properties.getProperty("username");
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Error when reading settings " +
                    "from filestorage.properties.");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }
}
