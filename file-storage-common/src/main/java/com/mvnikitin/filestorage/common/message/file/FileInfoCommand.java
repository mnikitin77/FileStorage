package com.mvnikitin.filestorage.common.message.file;

import java.util.Date;

public class FileInfoCommand extends FileAbstractCommand {
    private boolean isDirectory;
    private Date creationTime;
    private Date accessedTime;
    private Date modifiedTime;
    private long size;

    public FileInfoCommand(String fileName) {
        super(fileName);
    }

    public boolean isDirectory() {
        return isDirectory;
    }

    public void setDirectory(boolean directory) {
        isDirectory = directory;
    }

    public Date getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(Date creationTime) {
        this.creationTime = creationTime;
    }

    public Date getAccessedTime() {
        return accessedTime;
    }

    public void setAccessedTime(Date accessedTime) {
        this.accessedTime = accessedTime;
    }

    public Date getModifiedTime() {
        return modifiedTime;
    }

    public void setModifiedTime(Date modifiedTime) {
        this.modifiedTime = modifiedTime;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    @Override
    public String info() {
        return "get the information for " + getFileName();
    }
}
