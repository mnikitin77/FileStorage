package com.mvnikitin.filestorage.server.service;

import com.mvnikitin.filestorage.common.message.AbstractNetworkMessage;
import com.mvnikitin.filestorage.common.message.service.LogoffCommand;
import com.mvnikitin.filestorage.common.message.service.LogonCommand;
import com.mvnikitin.filestorage.common.message.service.RegisterCommand;

import java.sql.SQLException;
import java.util.*;
import java.util.function.Consumer;

public class ServiceCommandProcessUtils {
    private static Map<
            String,
            Consumer<AbstractNetworkMessage>>

            executors = new HashMap<>();

    static {
        executors.put(LogonCommand.class.getName(),
                ServiceCommandProcessUtils.createLogonExecutor());
        executors.put(LogoffCommand.class.getName(),
                ServiceCommandProcessUtils.createLogoffExecutor());
        executors.put(RegisterCommand.class.getName(),
                ServiceCommandProcessUtils.createRegisterExecutor());
    }

    public static void execute(AbstractNetworkMessage cmd) {
        String key = cmd.getClass().getName();
        if (executors.containsKey(key)) {
            executors.get(key).accept(cmd);
        } else {
            throw new IllegalArgumentException();
        }
    }

    private static Consumer<AbstractNetworkMessage>
    createLogonExecutor() {
        Consumer<AbstractNetworkMessage> consumer =
                (cmd) -> {
                    LogonCommand logonCmd =
                            (LogonCommand) cmd;
                    try {
                        if (AuthService.authenticate(logonCmd.getUsername(),
                                logonCmd.getPassword())) {
                            logonCmd.setResultCode(0);
                            logonCmd.setErrorText("OK");
                        } else {
                            logonCmd.setResultCode(-1);
                            logonCmd.setErrorText("Access denied.");
                        }
                    } catch (SQLException e) {
                        handleException(e, logonCmd);
                    }
                };

        return consumer;
    }

    private static Consumer<AbstractNetworkMessage>
    createLogoffExecutor() {
        Consumer<AbstractNetworkMessage> consumer =
                (cmd) -> {
                    LogoffCommand logoffCmd =
                            (LogoffCommand) cmd;
                    logoffCmd.setResultCode(0);
                    logoffCmd.setErrorText("OK");
                };

        return consumer;
    }

    private static Consumer<AbstractNetworkMessage>
    createRegisterExecutor() {
        Consumer<AbstractNetworkMessage> consumer =
                (cmd) -> {
                    RegisterCommand regCmd =
                            (RegisterCommand) cmd;
                    try {
                        if (AuthService.register(regCmd.getUsername(),
                                regCmd.getPassword())) {
                            regCmd.setResultCode(0);
                            regCmd.setErrorText("OK");
                        } else {
                            regCmd.setResultCode(-1);
                            regCmd.setErrorText("The user" +
                                    regCmd.getUsername() +
                                    " is already registered.");
                        }
                    } catch (SQLException e) {
                        handleException(e, regCmd);
                    }
                };

        return consumer;
    }

    private static void handleException(Exception e,
                                        AbstractNetworkMessage command) {
        command.setResultCode(-1);
        command.setErrorText(e.getMessage() == null ?
                "Error: " + e.toString() : e.getMessage());
        e.printStackTrace();
    }
}
