package com.mvnikitin.filestorage.common.message.file;

import com.mvnikitin.filestorage.common.message.AbstractNetworkMessage;

public abstract class FileAbstractCommand extends AbstractNetworkMessage {
    private String fileName;
    private boolean isOnClient; // Это костыль, но что делать

    public FileAbstractCommand(String fileName) {
        this.fileName = fileName;
        isOnClient = true;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public abstract void dummy();

    public boolean isOnClient() {
        return isOnClient;
    }

    public void setIsOnClient(boolean isOnClient) {
        this.isOnClient = isOnClient;
    }
}
