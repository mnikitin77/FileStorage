package com.mvnikitin.filestorage.server.service;

import com.mvnikitin.filestorage.common.message.AbstractNetworkMessage;
import com.mvnikitin.filestorage.common.message.service.FileServerConfigCommand;
import com.mvnikitin.filestorage.common.message.service.LogoffCommand;
import com.mvnikitin.filestorage.common.message.service.LogonCommand;
import com.mvnikitin.filestorage.common.message.service.RegisterCommand;
import com.mvnikitin.filestorage.common.utils.FileProcessConfig;

import java.sql.SQLException;
import java.util.*;
import java.util.function.BiConsumer;

public class ServiceCommandProcessUtils {
    private static Map<
            String,
            BiConsumer<AbstractNetworkMessage, FileProcessConfig>>

            executors = new HashMap<>();

    static {
        executors.put(FileServerConfigCommand.class.getName(),
                ServiceCommandProcessUtils.createFileServerConfigExecutor());
        executors.put(LogonCommand.class.getName(),
                ServiceCommandProcessUtils.createLogonExecutor());
        executors.put(LogoffCommand.class.getName(),
                ServiceCommandProcessUtils.createLogoffExecutor());
        executors.put(RegisterCommand.class.getName(),
                ServiceCommandProcessUtils.createRegisterExecutor());
    }

    public static void execute(AbstractNetworkMessage cmd,
                               FileProcessConfig config) {
        String key = cmd.getClass().getName();
        if (executors.containsKey(key)) {
            executors.get(key).accept(cmd, config);
        } else {
            throw new IllegalArgumentException();
        }
    }

    private static BiConsumer<AbstractNetworkMessage, FileProcessConfig>
    createFileServerConfigExecutor() {
        BiConsumer<AbstractNetworkMessage, FileProcessConfig> BiConsumer =
                (cmd, config) -> {
                    FileServerConfigCommand confCmd =
                            (FileServerConfigCommand) cmd;

                    confCmd.setBlockSize(config.getBlockSize());
                    confCmd.setResultCode(0);
                };

        return BiConsumer;
    }

    private static BiConsumer<AbstractNetworkMessage, FileProcessConfig>
    createLogonExecutor() {
        BiConsumer<AbstractNetworkMessage, FileProcessConfig> BiConsumer =
                (cmd, config) -> {
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

        return BiConsumer;
    }

    private static BiConsumer<AbstractNetworkMessage, FileProcessConfig>
    createLogoffExecutor() {
        BiConsumer<AbstractNetworkMessage, FileProcessConfig> BiConsumer =
                (cmd, config) -> {
                    LogoffCommand logoffCmd =
                            (LogoffCommand) cmd;
                    // TODO
                    System.out.println("User TODO logged off.");
                    logoffCmd.setResultCode(0);
                };

        return BiConsumer;
    }

    private static BiConsumer<AbstractNetworkMessage, FileProcessConfig>
    createRegisterExecutor() {
        BiConsumer<AbstractNetworkMessage, FileProcessConfig> BiConsumer =
                (cmd, config) -> {
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

        return BiConsumer;
    }

    private static void handleException(Exception e, AbstractNetworkMessage command) {
        command.setResultCode(-1);
        command.setErrorText(e.getMessage() == null ?
                "Error: " + e.toString() : e.getMessage());
        e.printStackTrace();
    }
}
