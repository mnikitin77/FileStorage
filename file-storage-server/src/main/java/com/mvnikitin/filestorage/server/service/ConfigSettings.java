package com.mvnikitin.filestorage.server.service;

import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Properties;
import java.util.stream.Collectors;

public class ConfigSettings extends ObjectPool<WorkArray>{
    private static final String PROPERTIES = "config.properties";

    private int port;
    private int maxTransferFileSizeMB;
    private String rootDirectory;
    private String tmpDirectory;
    private String userDB;
    private String JDBCDriver;
    private int blockSize;
    private int minBuffersNumber;

    private static ConfigSettings instance;

    public static ConfigSettings getInstance() {
        if (instance == null) {
            try {
                instance = new ConfigSettings();
                instance.init();
            } catch (SQLException | IOException e) {
                e.printStackTrace();
            }
        }
        return instance;
    }

    private class WorkArraysPoolMonitor extends Thread {
        private final static int POOL_CLEAN_FREQUENCY = 5000;

        @Override
        public void run() {
            try {
                while(true) {
                    sleep(POOL_CLEAN_FREQUENCY);
                    checkPool();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public int getPort() {
        return port;
    }

    public int getMaxTransferFileSizeMB() {
        return maxTransferFileSizeMB;
    }

    public String getTmpDirectory() {
        return tmpDirectory;
    }

    public String getRootDirectory() {
        return rootDirectory;
    }

    public int getBlockSize() {
        return blockSize;
    }

    private void init() throws IOException, SQLException {
        Properties properties = new Properties();
        properties.load(new FileReader("config.properties"));

        port = Integer.parseInt(properties.getProperty("port"));
        rootDirectory = properties.getProperty("root_dir");
        tmpDirectory = properties.getProperty("tmp_dir");
        userDB = properties.getProperty("user_db");
        JDBCDriver = properties.getProperty("jdbc_driver");
        blockSize = Integer.parseInt(
                properties.getProperty("transfer_buffer_size"));
        if(blockSize <= 0) {
            throw new RuntimeException(
                    "[local_array_size] property must be greater than 0.");
        }
        maxTransferFileSizeMB = Integer.parseInt(
                properties.getProperty("max_transfer_object_size_mb"));
        if(maxTransferFileSizeMB <= 0) {
            throw new RuntimeException(
                    "[max_transfer_object_size_mb] property must be greater than 0.");
        }
        minBuffersNumber = Integer.parseInt(
                properties.getProperty("min_transfer_buffers"));
        if(minBuffersNumber <= 0) {
            throw new RuntimeException(
                    "[min_transfer_buffers] property must be greater than 0.");
        }

        // Connect to the users DB
        DataService.connect(JDBCDriver, userDB);

        // Start monitoring the size of the pool of work arrays
        WorkArraysPoolMonitor poolMonitor = new WorkArraysPoolMonitor();
        poolMonitor.setDaemon(true);
        poolMonitor.start();
    }

    public void clearResources() {
        clear(); // clear the pool of WorkArrays.
        DataService.disconnect();
    }

    @Override
    protected WorkArray newObject() {
        return new WorkArray(blockSize);
    }

    @Override
    public void checkPool() {
        super.checkPool();
        System.out.println("checkPool() entered");

    // Check if the there are more than required of work arrays
        int requiredFree = minBuffersNumber - activeList.size();
        if (requiredFree > 0 && freeList.size() - requiredFree > 0) {
            freeList = freeList
                    .stream()
                    .limit(requiredFree)
                    .collect(Collectors.toList());
        }
        System.out.println("requiredFree: " + requiredFree +
                ", freeList.size(): " + freeList.size() +
                ", activeList.size(): " + activeList.size());
    }
}
