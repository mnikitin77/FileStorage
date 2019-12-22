package com.mvnikitin.filestorage.common.message.service;

import com.mvnikitin.filestorage.common.message.AbstractNetworkMessage;
import com.mvnikitin.filestorage.common.message.MessageType;

public class RegisterCommand extends LogonCommand {

    public RegisterCommand(String username, int password) {
        super(username, password);
    }

    @Override
    public void dummy() {

    }
}
