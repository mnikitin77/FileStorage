package com.mvnikitin.filestorage.common.utils;

public interface FileProcessConfig {

    String getRootDirectory();

    byte[] getLocalArray();

    default int getBlockSize() {
        return -1;
    };
}
