package com.mvnikitin.filestorage.client.utils;

import com.mvnikitin.filestorage.common.message.AbstractNetworkMessage;
import javafx.stage.Window;

import java.io.IOException;

public class ServerCommandSender {

    private Window owner;

    public void setOwner(Window owner) {
        this.owner = owner;
    }

    public AbstractNetworkMessage sendMessage(AbstractNetworkMessage msg) {
        AbstractNetworkMessage response = null;

        if (NetworkManager.sendMsg(msg)) {
            try {
                response = NetworkManager.readObject();
            } catch (ClassNotFoundException | IOException e) {
                UserNotifier.showErrorMessage("Network error",
                        "Error when receiving the response from the server.",
                        e.getMessage(),
                        owner);
            }

            if (response.getResultCode() != 0) {
                UserNotifier.showInfoMessage("Information",
                        "Unable to " + msg.info() + ".",
                        response.getErrorText(),
                        owner);
            } else {
                return response;
            }
        } else {
            UserNotifier.showErrorMessage("Network error",
                    "Unable to " + msg.info() + ".",
                    "Sending a message to the server failed.",
                    owner);
        }

        return null;
    }
}
