package com.mvnikitin.filestorage.common.message.service;

import com.mvnikitin.filestorage.common.message.AbstractNetworkMessage;
import com.mvnikitin.filestorage.common.message.MessageType;

public class FileServerConfigCommand extends AbstractNetworkMessage {
    private int blockSize;

    public FileServerConfigCommand() {
        super(MessageType.SERVICE);
    }

    public int getBlockSize() {
        return blockSize;
    }

    public void setBlockSize(int blockSize) {
        this.blockSize = blockSize;
    }

    @Override
    public void dummy() {

    }
}
