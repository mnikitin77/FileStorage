package com.mvnikitin.filestorage.common.message.file;

import com.mvnikitin.filestorage.common.message.AbstractNetworkMessage;
import com.mvnikitin.filestorage.common.message.MessageType;

public abstract class FileAbstractCommand extends AbstractNetworkMessage {
    private String fileName;
    private boolean isOnClient; // Это костыль, но что делать

    public FileAbstractCommand(String fileName) {
        super(MessageType.FILE);
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
