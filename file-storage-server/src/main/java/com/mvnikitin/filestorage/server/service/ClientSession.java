package com.mvnikitin.filestorage.server.service;

import com.mvnikitin.filestorage.common.utils.FileProcessData;

public class ClientSession implements FileProcessData {

    private String username;
    private String userDirectory;
    private String tmpDirectory;
    private String currentDirectory;

    public ClientSession(String username) {
        this.username = username;
        this.userDirectory = ConfigSettings.getInstance().getRootDirectory() +
                username + "/";;
        tmpDirectory = ConfigSettings.getInstance().getTmpDirectory() +
                username + "/";
        currentDirectory = userDirectory;
    }

    public String getUsername() {
        return username;
    }

    @Override
    public int getBlockSize() {
        return ConfigSettings.getInstance().getBlockSize();
    }

    @Override
    public String getCurrentDirectory() {
        return currentDirectory;
    }

    @Override
    public void setCurrentDirectory(String path) {
        currentDirectory = path;
    }

    @Override
    public String getRootDirectory() {
        return userDirectory;
    }

    @Override
    public byte[] getWorkArray() {
        WorkArray workArray = ConfigSettings.getInstance().getActiveElement();
        workArray.activate();

        byte[] array = workArray.getArray();
        workArray.deactivate();

        return array;
    }

    public String getTmpDirectory() {
        return tmpDirectory;
    }
}
