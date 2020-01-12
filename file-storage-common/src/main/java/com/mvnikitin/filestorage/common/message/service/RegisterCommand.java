package com.mvnikitin.filestorage.common.message.service;

public class RegisterCommand extends LogonCommand {

    public RegisterCommand(String username, int password) {
        super(username, password);
    }

    @Override
    public String info() {
        return "register a user";
    }
}
