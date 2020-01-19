package com.mvnikitin.filestorage.common.message.file;

public class FileChangeDirCommand extends FileAbstractCommand {

    public FileChangeDirCommand(String fileName) {
        super(fileName);
    }

    @Override
    public String info() {
        return "change the directory " + getFileName();
    }
}
