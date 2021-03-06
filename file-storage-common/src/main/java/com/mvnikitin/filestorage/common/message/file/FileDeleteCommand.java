package com.mvnikitin.filestorage.common.message.file;

public class FileDeleteCommand extends FileAbstractCommand {

    private boolean isDeleted;

    public FileDeleteCommand(String fileName) {
        super(fileName);
        isDeleted = false;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public void setDeleted(boolean deleted) {
        isDeleted = deleted;
    }

    @Override
    public String info() {
        return "delete " + getFileName();
    }
}
