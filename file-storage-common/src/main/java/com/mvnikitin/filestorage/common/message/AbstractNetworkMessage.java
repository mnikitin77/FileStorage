package com.mvnikitin.filestorage.common.message;

import java.io.Serializable;
import java.util.UUID;

public abstract class AbstractNetworkMessage implements Serializable {

    private String rqUID;
    private int resultCode;
    private String errorText;

    public AbstractNetworkMessage() {
        rqUID = UUID.randomUUID().toString();
    }

    public abstract void dummy(); //TODO

    public String getRqUID() {
        return rqUID;
    }

    public int getResultCode() {
        return resultCode;
    }

    public String getErrorText() {
        return errorText;
    }

    public void setResultCode(int resultCode) {
        this.resultCode = resultCode;
    }

    public void setErrorText(String errorText) {
        this.errorText = errorText;
    }
}
