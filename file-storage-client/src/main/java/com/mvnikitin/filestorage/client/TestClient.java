package com.mvnikitin.filestorage.client;

import com.mvnikitin.filestorage.client.utils.NetworkManager;
import com.mvnikitin.filestorage.common.message.AbstractNetworkMessage;
import com.mvnikitin.filestorage.common.message.NotSupportedCommand;
import com.mvnikitin.filestorage.common.message.file.*;
import com.mvnikitin.filestorage.common.message.service.GetConfigInfoCommand;
import com.mvnikitin.filestorage.common.message.service.LogoffCommand;
import com.mvnikitin.filestorage.common.message.service.LogonCommand;
import com.mvnikitin.filestorage.common.message.service.RegisterCommand;
import com.mvnikitin.filestorage.common.utils.FileCommandProcessUtils;
import com.mvnikitin.filestorage.common.utils.FileProcessData;

import java.io.IOException;
import java.util.List;

public class TestClient implements FileProcessData {
    private static final String DEFAULT_PATH = "client_storage/";

    private byte[] localArray;
    private int blockSize;

    public TestClient() throws IOException, ClassNotFoundException {
        NetworkManager.start("localhost", 8189);

        // Get some of the Server config parameters
        AbstractNetworkMessage msg = new GetConfigInfoCommand();
        NetworkManager.sendMsg(msg);
        msg = NetworkManager.readObject();

        blockSize = ((GetConfigInfoCommand)msg).getBlockSize();
        localArray = new byte[blockSize];
    }

    @Override
    public String getCurrentDirectory() {
        return DEFAULT_PATH;
    }

    @Override
    public String getRootDirectory() {
        return DEFAULT_PATH;
    }

    @Override
    public byte[] getWorkArray() {
        return localArray;
    }

    @Override
    public int getBlockSize() {
        return blockSize;
    }

    public static void main(String[] args)
            throws IOException, ClassNotFoundException {

        TestClient me = new TestClient();

        AbstractNetworkMessage msg = null;

    // Register a new user
        msg = new RegisterCommand("user", "Qwerty1".hashCode());
        NetworkManager.sendMsg(msg);
        msg = NetworkManager.readObject();
        System.out.println("Logon, user: " + ((RegisterCommand)msg).getUsername() +
                ", password: " + ((RegisterCommand)msg).getPassword());
        System.out.println("Result code: " + msg.getResultCode() + ", "
                + msg.getErrorText());

    // Register the same user once again
        msg = new RegisterCommand("user", "Qwerty1".hashCode());
        NetworkManager.sendMsg(msg);
        msg = NetworkManager.readObject();
        System.out.println("Logon, user: " + ((RegisterCommand)msg).getUsername() +
                ", password: " + ((RegisterCommand)msg).getPassword());
        System.out.println("Result code: " + msg.getResultCode() + ", "
                + msg.getErrorText());

        // Receive the list of files and directories.
        msg = new FileDirCommand();
        NetworkManager.sendMsg(msg);
        msg = NetworkManager.readObject();
        System.out.println("DIR, Error: " + msg.getErrorText() +
                ", code: " + msg.getResultCode());
        List<FileDirCommand.DirEntry> list1 =
                ((FileDirCommand)msg).getResults();
        if (list1 != null) {
            list1.stream().
                    map(dirEntry -> dirEntry.getDirectory() ?
                            "<" + dirEntry.getEntryName() + ">" :
                            dirEntry.getEntryName()).
                    forEach(System.out::println);
        }

    // Logging off
        msg = new LogoffCommand();
        NetworkManager.sendMsg(msg);
        msg = NetworkManager.readObject();
        System.out.println("Logoff current user, Result code: " +
                msg.getResultCode() + ", " + msg.getErrorText());

    // Logging on a user
        msg = new LogonCommand("test", 3556498);
//        msg = new LogonCommand("test1", 3556498);
        NetworkManager.sendMsg(msg);
        msg = NetworkManager.readObject();
        System.out.println("Logon, user: " + ((LogonCommand)msg).getUsername() +
                ", password: " + ((LogonCommand)msg).getPassword());
        System.out.println("Result code: " + msg.getResultCode() + ", "
                + msg.getErrorText());

        // Logging on again
        msg = new LogonCommand("test", 3556498);
        NetworkManager.sendMsg(msg);
        msg = NetworkManager.readObject();
        System.out.println("Logon, user: " + ((LogonCommand)msg).getUsername() +
                ", password: " + ((LogonCommand)msg).getPassword());
        System.out.println("Result code: " + msg.getResultCode() + ", "
                + msg.getErrorText());

    // Receive the list of files and directories.
        msg = new FileDirCommand();
        NetworkManager.sendMsg(msg);
        msg = NetworkManager.readObject();
        System.out.println("DIR, Error: " + msg.getErrorText() +
                ", code: " + msg.getResultCode());
        List<FileDirCommand.DirEntry> list =
                ((FileDirCommand)msg).getResults();
        list.stream().
                map(dirEntry -> dirEntry.getDirectory() ?
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
        if (((FIleTransferCommand) msg).getData() != null) {
            ((FIleTransferCommand) msg).setIsOnClient(true);
            FileCommandProcessUtils.execute((FileAbstractCommand) msg, me);
        }

    // Download a file by parts from the Server to the Client
        msg = new FIleTransferCommand("2.txt", false, me.getBlockSize());
//        msg = new FIleTransferCommand("2.txt", false, 1024 * 1024);

        boolean isComplete = false;
        while (!isComplete) {
            ((FIleTransferCommand)msg).setIsOnClient(true);
            NetworkManager.sendMsg(msg);
            msg = NetworkManager.readObject();
            FIleTransferCommand dwldCmd = (FIleTransferCommand)msg;
            dwldCmd.setIsOnClient(true);
            if (dwldCmd.getData() != null) {
                System.out.println("File " + dwldCmd.getFileName() + ": part " +
                        dwldCmd.getCurrentPartNumber() + " of " +
                        dwldCmd.getPartsCount() + " is downloaded. " +
                        dwldCmd.getData().length + " bytes transfered.");

                FileCommandProcessUtils.execute(dwldCmd, me);

                if (dwldCmd.getCurrentPartNumber() == dwldCmd.getPartsCount()) {
                    isComplete = true;
                    System.out.println("File " + dwldCmd.getFileName() +
                            " is downloaded successfully.");
                }

            } else {
                isComplete = true;
            }
        }

    // Upload a file by parts from the Client to the Server.
//        msg = new FIleTransferCommand("3.txt", true, me.getBlockSize());
        msg = new FIleTransferCommand("MOV_0546.mp4", true, me.getBlockSize());
        boolean sucess = false;
        do {
            FIleTransferCommand dwldCmd = (FIleTransferCommand)msg;
            dwldCmd.setIsOnClient(true);
            FileCommandProcessUtils.execute(dwldCmd, me);

            if (dwldCmd.getData() != null) {
                NetworkManager.sendMsg(msg);
                System.out.println("File " + dwldCmd.getFileName() + ": part " +
                        dwldCmd.getCurrentPartNumber() + " of " +
                        dwldCmd.getPartsCount() + " is uploaded. " +
                        dwldCmd.getData().length + " bytes transferred.");
                if (dwldCmd.getCurrentPartNumber() == dwldCmd.getPartsCount()) {
                    System.out.println("File " + dwldCmd.getFileName() +
                            " is uploadad successfully.");

                    sucess = true;
                    break;
                }

                msg = NetworkManager.readObject();
            }
            if (dwldCmd.getData() == null) {
                break;
            }
        } while (true);
    // Read the final response message sent by the server.
        if (sucess) {
            NetworkManager.readObject();
        }

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

        // Logging off
        msg = new LogoffCommand();
        NetworkManager.sendMsg(msg);
        msg = NetworkManager.readObject();
        System.out.println("Logoff current user, Result code: " +
                msg.getResultCode() + ", " + msg.getErrorText());


        NetworkManager.stop();
    }
}
