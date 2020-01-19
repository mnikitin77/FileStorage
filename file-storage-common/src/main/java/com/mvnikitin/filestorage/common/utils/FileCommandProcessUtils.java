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
            BiConsumer<FileAbstractCommand, FileProcessData>>

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
        executors.put(FileChangeDirCommand.class.getName(),
                FileCommandProcessUtils.createChangeDirExecutor());
        executors.put(FileMakeDirCommand.class.getName(),
                FileCommandProcessUtils.createMakeDirExecutor());
    }

    public static void execute(FileAbstractCommand cmd, FileProcessData processData) {
        String key = cmd.getClass().getName();
        if (executors.containsKey(key)) {
            executors.get(key).accept(cmd, processData);
        } else {
            throw new IllegalArgumentException();
        }
    }

    private static BiConsumer<FileAbstractCommand, FileProcessData>
    createDirExecutor() {
        return (cmd, processData) -> {
            List<FileDirCommand.DirEntry> result = new ArrayList<>();
            FileDirCommand dirCmd = (FileDirCommand) cmd;

            Path currentPath = Paths.get(
                    processData.getCurrentDirectory());

            try (DirectoryStream<Path> stream =
                         Files.newDirectoryStream(currentPath);) {

                BasicFileAttributes attrs;
                for (Path p: stream) {
                    attrs = Files.readAttributes(
                            p, BasicFileAttributes.class);

                    result.add(new FileDirCommand.DirEntry(p.getName(
                            p.getNameCount() - 1).toString(),
                            Files.isDirectory(p), attrs.size(),
                            new Date(attrs.creationTime().toMillis()),
                            new Date(attrs.lastModifiedTime().toMillis())));
                }

                dirCmd.setRelativePath(
                        Paths.get(processData.getRootDirectory())
                        .relativize(currentPath).toString());

            } catch (IOException e) {
                handleException(e, dirCmd);
            }

            dirCmd.setResults(result);
            dirCmd.setResultCode(0);
            dirCmd.setErrorText("OK");
        };
    }

    private static BiConsumer<FileAbstractCommand, FileProcessData>
    createAttrsExecutor() {
        return (cmd, processData) -> {
            FileInfoCommand attrCmd = (FileInfoCommand) cmd;
            BasicFileAttributes attrs;

            try {
                attrs = Files.readAttributes(Paths.get(
                        processData.getCurrentDirectory(), attrCmd.getFileName()),
                        BasicFileAttributes.class);

                attrCmd.setCreationTime(new Date(attrs.creationTime().toMillis()));
                attrCmd.setAccessedTime(new Date(attrs.lastAccessTime().toMillis()));
                attrCmd.setModifiedTime(new Date(attrs.lastModifiedTime().toMillis()));
                attrCmd.setDirectory(attrs.isDirectory());
                attrCmd.setSize(attrs.size());

                attrCmd.setResultCode(0);
                attrCmd.setErrorText("OK");
            } catch (IOException e) {
                handleException(e, attrCmd);
            }
        };
    }

    private static BiConsumer<FileAbstractCommand, FileProcessData>
    createFileTransferExecutor() {
        return (cmd, processData) -> {
            FIleTransferCommand dwldCmd = (FIleTransferCommand) cmd;

            try {
                if (dwldCmd.isUpload()  && !dwldCmd.isOnClient() ||
                        !dwldCmd.isUpload() && dwldCmd.isOnClient()) {
                    FileCommandProcessUtils.receiveFileData(dwldCmd,
                            processData.getCurrentDirectory());
                } else {
                    dwldCmd.setData(FileCommandProcessUtils.provideFileData(
                            dwldCmd,
                            processData.getCurrentDirectory(),
                            processData.getWorkArray()));
                }

                dwldCmd.setResultCode(0);
                dwldCmd.setErrorText("OK");
            } catch (IOException e) {
                handleException(e, dwldCmd);
            }
        };
    }

    private static byte[] provideFileData(FIleTransferCommand cmd,
                                          String directoryPath,
                                          byte[] array)
            throws IOException {

        // array is a big pre-created array to handle big files
        byte[] data = array;

        try (RandomAccessFile file = new RandomAccessFile(
                Paths.get(directoryPath,
                        cmd.getFileName()).toString(), "r");) {

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

                // Setting the pointer at the place we finished reading before.
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
                Paths.get(directoryPath,
                        cmd.getFileName()).toString(), "rw");) {
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

    private static BiConsumer<FileAbstractCommand, FileProcessData>
    createDeleteExecutor() {
        return (cmd, processData) -> {
            FileDeleteCommand delCmd = (FileDeleteCommand) cmd;

            try {
                delCmd.setDeleted(
                        Files.deleteIfExists(
                                Paths.get(processData.getCurrentDirectory(),
                                        delCmd.getFileName())));

                delCmd.setResultCode(0);
                delCmd.setErrorText("OK");
            } catch (IOException e) {
                handleException(e, delCmd);
            }
        };
    }

    private static BiConsumer<FileAbstractCommand, FileProcessData>
    createRenameExecutor() {
        return (cmd, processData) -> {
            FileRenameCommand renCmd = (FileRenameCommand) cmd;
            try {
                Path source = Paths.get(processData.getCurrentDirectory(),
                        renCmd.getFrom());
                Files.move(source, source.resolveSibling(
                        Paths.get(renCmd.getTo())));
                renCmd.setResultCode(0);
                renCmd.setErrorText("OK");
            } catch (IOException e) {
                handleException(e, renCmd);
            }
        };
    }

    private static BiConsumer<FileAbstractCommand, FileProcessData>
    createChangeDirExecutor() {
        return (cmd, processData) -> {
            FileChangeDirCommand cdCmd = (FileChangeDirCommand) cmd;

            String currentDir =
                    processData.getCurrentDirectory();

            if (cdCmd.getFileName() == null ||
                    cdCmd.getFileName().equals("")) {
                if (!currentDir.equals(
                        processData.getRootDirectory())) {

                    processData.setCurrentDirectory(
                            Paths.get(currentDir)
                                    .getParent().toString());

                    cdCmd.setResultCode(0);
                    cdCmd.setErrorText("OK");
                } else {
                    cdCmd.setResultCode(-1);
                    cdCmd.setErrorText("The current folder is " +
                            "the root directory.");
                }
            } else {
                processData.setCurrentDirectory(
                        Paths.get(currentDir, cdCmd.getFileName())
                                .toString());
                cdCmd.setResultCode(0);
                cdCmd.setErrorText("OK");
            }
        };
    }

    private static BiConsumer<FileAbstractCommand, FileProcessData>
    createMakeDirExecutor() {
        return (cmd, processData) -> {
            FileMakeDirCommand mdCmd = (FileMakeDirCommand) cmd;
            try {
                Files.createDirectory(
                        Paths.get(processData.getCurrentDirectory(),
                                mdCmd.getFileName()));
                mdCmd.setResultCode(0);
                mdCmd.setErrorText("OK");
            } catch (IOException e) {
                handleException(e, mdCmd);
            }
        };
    }

    private static void handleException(Exception e, AbstractNetworkMessage command) {
        command.setResultCode(-1);
        command.setErrorText(e.getMessage() == null ?
                "Error: " + e.toString() : e.getMessage());
        e.printStackTrace();
    }
}
