package com.mvnikitin.filestorage.common.utils;

import com.mvnikitin.filestorage.common.message.file.*;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.function.BiConsumer;

public class CommandProcessUtils {

    private static Map<String, BiConsumer<FileAbstractCommand, String>>
            executors = new HashMap<>();
    static {
        executors.put(FileDirCommand.class.getName(),
                CommandProcessUtils.createDirExecutor());
        executors.put(FileInfoCommand.class.getName(),
                CommandProcessUtils.createAttrsExecutor());
        executors.put(FIleTransferCommand.class.getName(),
                CommandProcessUtils.createFileTransferExecutor());
        executors.put(FileDeleteCommand.class.getName(),
                CommandProcessUtils.createDeleteExecutor());
        executors.put(FileRenameCommand.class.getName(),
                CommandProcessUtils.createRenameExecutor());
    }

    public static void execute(FileAbstractCommand cmd, String directoryPath) {
        String key = cmd.getClass().getName();
        if (executors.containsKey(key)) {
            executors.get(key).accept(cmd, directoryPath);
        } else {
            throw new IllegalArgumentException();
        }
    }

    private static BiConsumer<FileAbstractCommand, String> createDirExecutor() {
        BiConsumer<FileAbstractCommand, String> biConsumer =
                (cmd, directoryPath) -> {
                    List<FileDirCommand.DirEntry> result = new ArrayList<>();
                    FileDirCommand dirCmd = (FileDirCommand) cmd;

                    try (DirectoryStream<Path> stream =
                                 Files.newDirectoryStream(
                                         Paths.get(directoryPath));) {
                        for (Path p: stream) {
                            result.add(new FileDirCommand.DirEntry(p.getName(
                                    p.getNameCount() - 1).toString(),
                                    Files.isDirectory(p)));
                        }
                    } catch (IOException e) {
                        handleException(e, dirCmd);
                    }

                    dirCmd.setResults(result);
                    dirCmd.setResultCode(0);
                };

        return biConsumer;
    }

    private static BiConsumer<FileAbstractCommand, String>
    createAttrsExecutor() {
        BiConsumer<FileAbstractCommand, String> biConsumer =
                (cmd, directoryPath) -> {
            FileInfoCommand attrCmd = (FileInfoCommand) cmd;
            BasicFileAttributes attrs;

            try {
                attrs = Files.readAttributes(Paths.get(
                        directoryPath + attrCmd.getFileName()),
                        BasicFileAttributes.class);

                attrCmd.setCreationTime(new Date(attrs.creationTime().toMillis()));
                attrCmd.setAccessedTime(new Date(attrs.lastAccessTime().toMillis()));
                attrCmd.setModifiedTime(new Date(attrs.lastModifiedTime().toMillis()));
                attrCmd.setDirectory(attrs.isDirectory());
                attrCmd.setSize(attrs.size());
                attrCmd.setResultCode(0);

            } catch (IOException e) {
                handleException(e, attrCmd);
            }
        };

        return biConsumer;
    }

    private static BiConsumer<FileAbstractCommand, String>
    createFileTransferExecutor() {
        BiConsumer<FileAbstractCommand, String> biConsumer =
                (cmd, directoryPath) -> {
            FIleTransferCommand dwldCmd = (FIleTransferCommand) cmd;

            try {
                if (dwldCmd.isUpload()  && !dwldCmd.isOnClient() ||
                        !dwldCmd.isUpload() && dwldCmd.isOnClient()) {
                    CommandProcessUtils.receiveFileData(dwldCmd,
                            directoryPath);
                } else {
                    dwldCmd.setData(CommandProcessUtils.provideFileData(
                            dwldCmd, directoryPath));
                }

                dwldCmd.setResultCode(0);

            } catch (IOException e) {
                handleException(e, dwldCmd);
            }
        };
        return biConsumer;
    }

    private static byte[] provideFileData(FIleTransferCommand cmd,
                                          String directoryPath)
            throws IOException {
        byte[] data = null;

        try (RandomAccessFile file = new RandomAccessFile(
                directoryPath + cmd.getFileName(), "r");) {
            if (cmd.getBlockSize() <= 0) {
                if (file.length() > Integer.MAX_VALUE) {
                    throw new RuntimeException(
                            "The file is too large to download at once");
                }
                data = new byte[(int) file.length()];
                file.readFully(data);

            } else {
                long fileLength = file.length();
                if (fileLength < cmd.getBlockSize()) {
                    cmd.setBlockSize((int) fileLength);
                }
                int blockSize = cmd.getBlockSize();
                if (cmd.getPartsCount() <= 0) {
                    cmd.setPartsCount(
                            (int) (fileLength % blockSize == 0 ?
                                    fileLength / blockSize :
                                    fileLength / blockSize + 1)
                    );
                }
                cmd.incrementCurrentPartNumber();
                int currentPart = cmd.getCurrentPartNumber();

                int partLength = blockSize;
                if (currentPart == cmd.getPartsCount()) {
                    partLength = (int) (fileLength - blockSize * (currentPart - 1));
                }

                data = new byte[partLength];
                // Setting the poiter at the place we finished reading before.
                file.seek(blockSize * (currentPart - 1));
                file.read(data, 0, partLength);
            }
        }

        return data;
    }

    private static void receiveFileData(FIleTransferCommand cmd,
                                        String directoryPath)
            throws IOException {

        byte[] data = null;

        try (RandomAccessFile file = new RandomAccessFile(
                directoryPath + cmd.getFileName(), "rw");) {
            if (cmd.getBlockSize() <= 0) {
                file.write(cmd.getData());
            } else {
                int blockSize = cmd.getBlockSize();
                int currentPart = cmd.getCurrentPartNumber();
                // There's no the current part's data in the file yet.
                file.seek(blockSize * (currentPart - 1));
                file.write(cmd.getData());
            }
            // In order to not to send back unnecessarily the same data.
            cmd.setData(null);
        }
    }

    private static BiConsumer<FileAbstractCommand, String>
    createDeleteExecutor() {
        BiConsumer<FileAbstractCommand, String> biConsumer =
                (cmd, directoryPath) -> {
                    FileDeleteCommand delCmd = (FileDeleteCommand) cmd;

                    try {
                        delCmd.setDeleted(
                                Files.deleteIfExists(
                                        Paths.get(directoryPath +
                                                delCmd.getFileName())));
                        delCmd.setResultCode(0);
                    } catch (IOException e) {
                        handleException(e, delCmd);
                    }
                };

        return biConsumer;
    }

    private static BiConsumer<FileAbstractCommand, String>
    createRenameExecutor() {
        BiConsumer<FileAbstractCommand, String> biConsumer =
                (cmd, directoryPath) -> {
                    FileRenameCommand renCmd = (FileRenameCommand) cmd;

                    try {
                        Path source = Paths.get(directoryPath +
                                renCmd.getFrom());
                        Path test = Files.move(source, source.resolveSibling(
                                Paths.get(renCmd.getTo())));
                        renCmd.setResultCode(0);
                    } catch (IOException e) {
                        handleException(e, renCmd);
                    }
                };

        return biConsumer;
    }

    private static void handleException(Exception e, FileAbstractCommand command) {
        command.setResultCode(-1);
        command.setErrorText(e.getMessage() == null ?
                "Error: " + e.toString() : e.getMessage());
        e.printStackTrace();
    }
}
