package com.mvnikitin.filestorage.client;

import com.mvnikitin.filestorage.client.utils.NetworkManager;
import com.mvnikitin.filestorage.common.message.AbstractNetworkMessage;
import com.mvnikitin.filestorage.common.message.NotSupportedCommand;
import com.mvnikitin.filestorage.common.message.file.*;
import com.mvnikitin.filestorage.common.utils.CommandProcessUtils;

import java.io.IOException;
import java.util.List;

public class TestClient{
    private static final String DEFAULT_PATH = "client_storage/";

    public static void main(String[] args)
            throws IOException, ClassNotFoundException {
        NetworkManager.start("localhost", 8189);

    // Receive the list of files and directories.
        AbstractNetworkMessage msg = new FileDirCommand();
        NetworkManager.sendMsg(msg);
        msg = NetworkManager.readObject();
        System.out.println("Command UID: " + msg.getRqUID());
        System.out.println("Error: " + msg.getErrorText() +
                ", code: " + msg.getResultCode());
        List<FileDirCommand.DirEntry> list =
                ((FileDirCommand)msg).getResults();
        list.stream().
                map(dirEntry -> dirEntry.isDirectory() ?
                        "<" + dirEntry.getEntryName() + ">" :
                        dirEntry.getEntryName()).
                forEach(System.out::println);

    // Get attributes of a file.
        msg = new FileInfoCommand("1.txt");
        NetworkManager.sendMsg(msg);
        msg = NetworkManager.readObject();
        System.out.println(msg.getRqUID());
        System.out.println("Error: " + msg.getErrorText() +
                ", code: " + msg.getResultCode());
        System.out.println("size: " +
                ((FileInfoCommand)msg).getSize());
        System.out.println("is directory: " +
                ((FileInfoCommand)msg).isDirectory());
        System.out.println("created: " +
                ((FileInfoCommand)msg).getCreationTime());
        System.out.println("accessed: " +
                ((FileInfoCommand)msg).getAccessedTime());
        System.out.println("modified: " +
                ((FileInfoCommand)msg).getModifiedTime());

    // Execute a not supported command.
        AbstractNetworkMessage notSupported = new NotSupportedCommand();
        NetworkManager.sendMsg(notSupported);
        notSupported = NetworkManager.readObject();
        System.out.println("ResultCode: " + notSupported.getResultCode() +
                ", Error: " + notSupported.getErrorText());

    // Download a file that does not exist.
        msg = new FIleTransferCommand("Такого файла нет.txt", false);
        NetworkManager.sendMsg(msg);
        msg = NetworkManager.readObject();
        FIleTransferCommand cmd = (FIleTransferCommand)msg;
        System.out.println("ResultCode: " + cmd.getResultCode() +
                ", Error: " + cmd.getErrorText());

    // Download a file in a single message from the Server to the Client.
        msg = new FIleTransferCommand("1.txt", false);
        ((FIleTransferCommand)msg).setIsOnClient(true);
        NetworkManager.sendMsg(msg);
        msg = NetworkManager.readObject();
        ((FIleTransferCommand)msg).setIsOnClient(true);
        CommandProcessUtils.execute((FileAbstractCommand) msg, DEFAULT_PATH);

    // Download a file by parts from the Server to the Client
        msg = new FIleTransferCommand("2.txt", false, 16);
//        msg = new FIleTransferCommand("2.txt", false, 1024 * 1024);

        boolean isComplete = false;
        while (!isComplete) {
            ((FIleTransferCommand)msg).setIsOnClient(true);
            NetworkManager.sendMsg(msg);
            msg = NetworkManager.readObject();
            FIleTransferCommand dwldCmd = (FIleTransferCommand)msg;
            dwldCmd.setIsOnClient(true);
            System.out.println("File " + dwldCmd.getFileName() + ": part " +
                    dwldCmd.getCurrentPartNumber() + " of " +
                    dwldCmd.getPartsCount() + " is downloaded. " +
                    dwldCmd.getData().length + " bytes transferred.");
            CommandProcessUtils.execute(dwldCmd, DEFAULT_PATH);
            if (dwldCmd.getCurrentPartNumber() == dwldCmd.getPartsCount()) {
                isComplete = true;
                System.out.println("File " + dwldCmd.getFileName() +
                        " is downloaded successfully.");
            }
        }

    // Upload a file by parts from the Client to the Server.
        msg = new FIleTransferCommand("3.txt", true, 64);
//        msg = new FIleTransferCommand("3.txt", true, 1024 * 1024);
        do {
            FIleTransferCommand dwldCmd = (FIleTransferCommand)msg;
            dwldCmd.setIsOnClient(true);
            CommandProcessUtils.execute(dwldCmd, DEFAULT_PATH);
            NetworkManager.sendMsg(msg);
            System.out.println("File " + dwldCmd.getFileName() + ": part " +
                    dwldCmd.getCurrentPartNumber() + " of " +
                    dwldCmd.getPartsCount() + " is uploaded. " +
                    dwldCmd.getData().length + " bytes transferred.");
            if (dwldCmd.getCurrentPartNumber() == dwldCmd.getPartsCount()) {
                System.out.println("File " + dwldCmd.getFileName() +
                        " is uploadad successfully.");
                break;
            }
            msg = NetworkManager.readObject();
        } while (true);
    // Read the final response message sent by the server.
        NetworkManager.readObject();

    // Deleting a file or directory
        msg = new FileDeleteCommand("somedir");
        NetworkManager.sendMsg(msg);
        msg = NetworkManager.readObject();
        FileDeleteCommand delCmd = (FileDeleteCommand) msg;
        System.out.println("File or directory [" + delCmd.getFileName() +
                "] is deleted: " + delCmd.isDeleted() + ". ResultCode: " +
                delCmd.getResultCode() + ", Error: " + delCmd.getErrorText());

        msg = new FileDeleteCommand("тест");
        NetworkManager.sendMsg(msg);
        msg = NetworkManager.readObject();
        delCmd = (FileDeleteCommand) msg;
        System.out.println("File or directory [" + delCmd.getFileName() +
                "] is deleted: " + delCmd.isDeleted() + ". ResultCode: " +
                delCmd.getResultCode() + ", Error: " + delCmd.getErrorText());

    // Rename a file
        msg = new FileRenameCommand("one.txt", "two.txt");
//        msg = new FileRenameCommand("two.txt", "two.txt");
        NetworkManager.sendMsg(msg);
        msg = NetworkManager.readObject();
        FileRenameCommand renCmd = (FileRenameCommand) msg;
        System.out.println("File or directory [" + renCmd.getFrom() +
                "] is renamed: " + (renCmd.getResultCode() == 0) +
                ". ResultCode: " + renCmd.getResultCode() + ", Error: "
                + renCmd.getErrorText());

    // Rename a file which does not exist.
        msg = new FileRenameCommand("xyz.txt", "zyx.txt");
        NetworkManager.sendMsg(msg);
        msg = NetworkManager.readObject();
        renCmd = (FileRenameCommand) msg;
        System.out.println("File or directory [" + renCmd.getFrom() +
                "] is renamed: " + (renCmd.getResultCode() == 0) +
                ". ResultCode: " + renCmd.getResultCode() + ", Error: "
                + renCmd.getErrorText());

    // Rename a directory.
        msg = new FileRenameCommand("two", "three");
//        msg = new FileRenameCommand("three", "three");
        NetworkManager.sendMsg(msg);
        msg = NetworkManager.readObject();
        renCmd = (FileRenameCommand) msg;
        System.out.println("File or directory [" + renCmd.getFrom() +
                "] is renamed: " + (renCmd.getResultCode() == 0) +
                ". ResultCode: " + renCmd.getResultCode() + ", Error: "
                + renCmd.getErrorText());

        NetworkManager.stop();
    }
}
