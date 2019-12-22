package com.mvnikitin.filestorage.common.message.service;

import com.mvnikitin.filestorage.common.message.AbstractNetworkMessage;
import com.mvnikitin.filestorage.common.message.MessageType;

public class LogonCommand extends AbstractNetworkMessage {

    private String username;
    private int password;

    public LogonCommand(String username, int password)
    {
        super(MessageType.SERVICE);
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public int getPassword() {
        return password;
    }

    @Override
    public void dummy() {

    }
}
