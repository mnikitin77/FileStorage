package com.mvnikitin.filestorage.common.message.file;

public class FileRenameCommand extends FileAbstractCommand {
    private String to;

    public FileRenameCommand(String from, String to) {
        super(from);
        this.to = to;
    }

    public String getFrom() {
        return super.getFileName();
    }

    public String getTo() {
        return to;
    }

    @Override
    public void dummy() {

    }
}
