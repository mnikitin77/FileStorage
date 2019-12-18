package com.mvnikitin.filestorage.server;

import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

public class ConfigSettings {
    private static final String PROPERTIES = "config.properties";

    private int port;
    private int maxTransferFileSizeMB;
    private String rootDirectory;

    private static ConfigSettings instance;

    public static ConfigSettings getInstance() throws IOException {
        if (instance == null) {
            instance = new ConfigSettings();
            instance.loadProperties();
        }
        return instance;
    }

    public int getPort() {
        return port;
    }

    public int getMaxTransferFileSizeMB() {
        return maxTransferFileSizeMB;
    }

    public String getRootDirectory() {
        return rootDirectory;
    }

    private void loadProperties() throws IOException {
        Properties properties = new Properties();
        properties.load(new FileReader("config.properties"));
        port = Integer.parseInt(properties.getProperty("port"));
        maxTransferFileSizeMB = Integer.parseInt(
                properties.getProperty("max_transfer_object_size_mb"));
        rootDirectory = properties.getProperty("root_dir");
    }
}
