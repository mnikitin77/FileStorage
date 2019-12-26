package com.mvnikitin.filestorage.server.service;

import com.mvnikitin.filestorage.common.utils.FileProcessData;

public class ClientSession implements FileProcessData {

    private String username;
    private String userDirectory;
    private String tmpDirectory;

    public ClientSession(String username) {
        this.username = username;
        this.userDirectory = ConfigSettings.getInstance().getRootDirectory() +
                username + "/";;
        tmpDirectory = ConfigSettings.getInstance().getTmpDirectory() +
                username + "/";
    }

    public String getUsername() {
        return username;
    }

    @Override
    public int getBlockSize() {
        return ConfigSettings.getInstance().getBlockSize();
    }

    @Override
    public String getDirectoryPath() {
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
