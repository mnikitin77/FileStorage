package com.mvnikitin.filestorage.common.message.service;

import com.mvnikitin.filestorage.common.message.AbstractNetworkMessage;
import com.mvnikitin.filestorage.common.message.MessageType;

public class LogoffCommand extends AbstractNetworkMessage {

    public LogoffCommand() {
        super(MessageType.SERVICE);
    }

    @Override
    public String info() {
        return "log out of the server";
    }
}
