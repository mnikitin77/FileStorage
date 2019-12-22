package com.mvnikitin.filestorage.common.utils;

import com.mvnikitin.filestorage.common.message.AbstractNetworkMessage;
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

public class FileCommandProcessUtils {

    private static Map<
            String,
            BiConsumer<FileAbstractCommand, FileProcessConfig>>

            executors = new HashMap<>();

    static {
        executors.put(FileDirCommand.class.getName(),
                FileCommandProcessUtils.createDirExecutor());
        executors.put(FileInfoCommand.class.getName(),
                FileCommandProcessUtils.createAttrsExecutor());
        executors.put(FIleTransferCommand.class.getName(),
                FileCommandProcessUtils.createFileTransferExecutor());
        executors.put(FileDeleteCommand.class.getName(),
                FileCommandProcessUtils.createDeleteExecutor());
        executors.put(FileRenameCommand.class.getName(),
                FileCommandProcessUtils.createRenameExecutor());
    }

    public static void execute(FileAbstractCommand cmd, FileProcessConfig config) {
        String key = cmd.getClass().getName();
        if (executors.containsKey(key)) {
            executors.get(key).accept(cmd, config);
        } else {
            throw new IllegalArgumentException();
        }
    }

    private static BiConsumer<FileAbstractCommand, FileProcessConfig>
    createDirExecutor() {
        BiConsumer<FileAbstractCommand, FileProcessConfig> biConsumer =
                (cmd, config) -> {
                    List<FileDirCommand.DirEntry> result = new ArrayList<>();
                    FileDirCommand dirCmd = (FileDirCommand) cmd;

                    try (DirectoryStream<Path> stream =
                                 Files.newDirectoryStream(Paths.get(
                                         config.getRootDirectory()));) {
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

    private static BiConsumer<FileAbstractCommand, FileProcessConfig>
    createAttrsExecutor() {
        BiConsumer<FileAbstractCommand, FileProcessConfig> biConsumer =
                (cmd, config) -> {
            FileInfoCommand attrCmd = (FileInfoCommand) cmd;
            BasicFileAttributes attrs;

            try {
                attrs = Files.readAttributes(Paths.get(
                        config.getRootDirectory() + attrCmd.getFileName()),
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

    private static BiConsumer<FileAbstractCommand, FileProcessConfig>
    createFileTransferExecutor() {
        BiConsumer<FileAbstractCommand, FileProcessConfig> biConsumer =
                (cmd, config) -> {
            FIleTransferCommand dwldCmd = (FIleTransferCommand) cmd;

            try {
                if (dwldCmd.isUpload()  && !dwldCmd.isOnClient() ||
                        !dwldCmd.isUpload() && dwldCmd.isOnClient()) {
                    FileCommandProcessUtils.receiveFileData(dwldCmd,
                            config.getRootDirectory());
                } else {
                    dwldCmd.setData(FileCommandProcessUtils.provideFileData(
                            dwldCmd,
                            config.getRootDirectory(),
                            config.getLocalArray()));
                }

                dwldCmd.setResultCode(0);

            } catch (IOException e) {
                handleException(e, dwldCmd);
            }
        };
        return biConsumer;
    }

    private static byte[] provideFileData(FIleTransferCommand cmd,
                                          String directoryPath,
                                          byte[] array)
            throws IOException {

        // array is a big pre-created array to handle big files
        byte[] data = array;

        try (RandomAccessFile file = new RandomAccessFile(
                directoryPath + cmd.getFileName(), "r");) {

            int fileLength = (int)file.length();
            if (fileLength < array.length) {
                data = new byte[fileLength];
            }

            if (cmd.getBlockSize() <= 0) {
                if (fileLength > Integer.MAX_VALUE) {
                    throw new RuntimeException(
                            "The file is too large to download at once");
                }
                if (fileLength > data.length) {
                    data = new byte[(int) file.length()];
                }
                file.readFully(data, 0, fileLength);

            } else {
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
                    data = new byte[partLength];
                }

//                data = new byte[partLength];
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

    private static BiConsumer<FileAbstractCommand, FileProcessConfig>
    createDeleteExecutor() {
        BiConsumer<FileAbstractCommand, FileProcessConfig> biConsumer =
                (cmd, config) -> {
                    FileDeleteCommand delCmd = (FileDeleteCommand) cmd;

                    try {
                        delCmd.setDeleted(
                                Files.deleteIfExists(
                                        Paths.get(config.getRootDirectory() +
                                                delCmd.getFileName())));
                        delCmd.setResultCode(0);
                    } catch (IOException e) {
                        handleException(e, delCmd);
                    }
                };

        return biConsumer;
    }

    private static BiConsumer<FileAbstractCommand, FileProcessConfig>
    createRenameExecutor() {
        BiConsumer<FileAbstractCommand, FileProcessConfig> biConsumer =
                (cmd, config) -> {
                    FileRenameCommand renCmd = (FileRenameCommand) cmd;

                    try {
                        Path source = Paths.get(config.getRootDirectory() +
                                renCmd.getFrom());
                        Files.move(source, source.resolveSibling(
                                Paths.get(renCmd.getTo())));
                        renCmd.setResultCode(0);
                    } catch (IOException e) {
                        handleException(e, renCmd);
                    }
                };

        return biConsumer;
    }

    private static void handleException(Exception e, AbstractNetworkMessage command) {
        command.setResultCode(-1);
        command.setErrorText(e.getMessage() == null ?
                "Error: " + e.toString() : e.getMessage());
        e.printStackTrace();
    }
}
