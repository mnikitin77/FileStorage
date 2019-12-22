package com.mvnikitin.filestorage.server.service;

import com.mvnikitin.filestorage.common.utils.FileProcessConfig;

import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Properties;

public class ConfigSettings implements FileProcessConfig {
    private static final String PROPERTIES = "config.properties";

    private int port;
    private int maxTransferFileSizeMB;
    private String rootDirectory;
    private String userDB;
    private String JDBCDriver;
    private int blockSize;
    private byte[] localArray;

    private static ConfigSettings instance;

    public static ConfigSettings getInstance()
            throws IOException, SQLException {
        if (instance == null) {
            instance = new ConfigSettings();
            instance.init();
        }
        return instance;
    }

    public int getPort() {
        return port;
    }

    public int getMaxTransferFileSizeMB() {
        return maxTransferFileSizeMB;
    }

    @Override
    public String getRootDirectory() {
        return rootDirectory;
    }

    @Override
    public byte[] getLocalArray() {
        return localArray;
    }

    @Override
    public int getBlockSize() {
        return blockSize;
    }

    private void init() throws IOException, SQLException {
        Properties properties = new Properties();
        properties.load(new FileReader("config.properties"));
        port = Integer.parseInt(properties.getProperty("port"));
        maxTransferFileSizeMB = Integer.parseInt(
                properties.getProperty("max_transfer_object_size_mb"));
        rootDirectory = properties.getProperty("root_dir");
        userDB = properties.getProperty("user_db");
        JDBCDriver = properties.getProperty("jdbc_driver");
        blockSize = Integer.parseInt(
                properties.getProperty("local_array_size"));
        if(blockSize > 0) {
            localArray = new byte[blockSize];
        } else {
            throw new RuntimeException(
                    "[local_array_size] property must be greater than 0.");
        }

        // Connect to the users DB
        DataService.connect(JDBCDriver, userDB);
    }

    public static void clearResources() {
        DataService.disconnect();
    }
}
