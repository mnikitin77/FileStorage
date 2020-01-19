package com.mvnikitin.filestorage.server.service;

import com.mvnikitin.filestorage.common.message.AbstractNetworkMessage;
import com.mvnikitin.filestorage.common.message.service.LogoffCommand;
import com.mvnikitin.filestorage.common.message.service.LogonCommand;
import com.mvnikitin.filestorage.common.message.service.RegisterCommand;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ServiceCommandProcessUtils {
    private static final Logger logger =
            LogManager.getLogger(ServiceCommandProcessUtils.class.getName());

    private static Map<String, Consumer<AbstractNetworkMessage>> executors =
            new HashMap<>();

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
                            logonCmd.setErrorText("Invalid username or password.");
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

                        // Create the user's root directory
                            Files.createDirectory(Paths.get(ConfigSettings
                                            .getInstance()
                                            .getRootDirectory(),
                                    regCmd.getUsername()));

                            regCmd.setResultCode(0);
                            regCmd.setErrorText("OK");
                        } else {
                            regCmd.setResultCode(-1);
                            regCmd.setErrorText("The user " +
                                    regCmd.getUsername() +
                                    " is already registered.");
                        }
                    } catch (SQLException | IOException e) {
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
        logger.error(command.getErrorText(), e);
    }
}
