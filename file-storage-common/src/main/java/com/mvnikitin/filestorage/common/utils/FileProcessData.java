package com.mvnikitin.filestorage.common.utils;

public interface FileProcessData {

    String getCurrentDirectory();

    byte[] getWorkArray();

    default int getBlockSize() {
        return -1;
    };

    default String getRootDirectory() {
        return null;
    }

    default void setCurrentDirectory(String path) {
    }
}
