package com.mvnikitin.filestorage.common.message;

public class NotSupportedCommand extends AbstractNetworkMessage {

    public NotSupportedCommand() {
        super(MessageType.SERVICE);
    }

    @Override
    public String info() {
        return "not supported command";
    }
}
