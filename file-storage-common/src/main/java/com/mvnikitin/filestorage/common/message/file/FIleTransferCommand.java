package com.mvnikitin.filestorage.common.message.file;

public class FIleTransferCommand extends FileAbstractCommand {

    private int partsCount;
    private int currentPartNumber;
    private int blockSize;
    private byte[] data;
    private boolean isUpload;

    public FIleTransferCommand(String fileName, boolean isUpload) {
        super(fileName);
        this.partsCount = 0;
        this.currentPartNumber = 0;
        this.blockSize = -1;
        this.isUpload = isUpload;
        setIsOnClient(true);
    }

    public FIleTransferCommand(String fileName, boolean isUpload, int blockSize) {
        this(fileName, isUpload);
        this.blockSize = blockSize;
    }

    public int getPartsCount() {
        return partsCount;
    }

    public void setPartsCount(int partsCount) {
        this.partsCount = partsCount;
    }

    public void incrementCurrentPartNumber(){
        currentPartNumber++;
    }

    public int getCurrentPartNumber() {
        return currentPartNumber;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public int getBlockSize() {
        return blockSize;
    }

    public void setBlockSize(int blockSize) {
        this.blockSize = blockSize;
    }

    public boolean isUpload() {
        return isUpload;
    }

    @Override
    public String info() {
        return "transfer the file " + getFileName();
    }
}
