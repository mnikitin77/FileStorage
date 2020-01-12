package com.mvnikitin.filestorage.common.message.file;

public class FileMakeDirCommand extends FileAbstractCommand {

    public FileMakeDirCommand(String fileName) {
        super(fileName);
    }

    @Override
    public String info() {
        return "create the folder " + getFileName();
    }
}
