package com.mvnikitin.filestorage.server.service;

public class WorkArray implements Poolable {
    private byte[] array;
    private boolean isActive;

    public WorkArray(int size) {
        array = new byte[size];
        isActive = false;
    }

    public byte[] getArray() {
        return array;
    }

    @Override
    public boolean isActive() {
        return isActive;
    }

    public void activate() {
        isActive = true;
    }

    public void deactivate() {
        isActive = false;
    }
}
