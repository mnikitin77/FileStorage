package com.mvnikitin.filestorage.common.utils;

public interface FileProcessData {

    String getDirectoryPath();

    byte[] getWorkArray();

    default int getBlockSize() {
        return -1;
    };
}
