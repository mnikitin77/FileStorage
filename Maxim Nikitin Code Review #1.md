# Серверная часть
## Пакет com.mvnikitin.filestorage.server
### Файл FileStorageServer.java

package com.mvnikitin.filestorage.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;

public class FileStorageServer {

    private static int maxMessageSize;
    private static int port;

    public void run() throws Exception {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel)
                                throws Exception {
                            socketChannel.pipeline().addLast(
                                    new ObjectDecoder(
                                            maxMessageSize,
                                            ClassResolvers.cacheDisabled(null)),
                                    new ObjectEncoder(),
                                    new MessageHandler()
                            );
                        }
                    })
                    .childOption(ChannelOption.SO_KEEPALIVE, true);
            ChannelFuture future =
                    b.bind(port).sync();
            future.channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    public static void main(String[] args) throws Exception {
        maxMessageSize = 1024 * 1024 *
                ConfigSettings.getInstance().getMaxTransferFileSizeMB();
        try {
            if (args.length > 0) {
                port = Integer.parseInt(args[0]);
                if (port < 1024 || port > 65535) {
                    throw new RuntimeException();
                }
            } else {
                port = ConfigSettings.getInstance().getPort();
            }
        } catch (Exception e) {
            throw new RuntimeException("Invalid port number. " +
                    "The port must be in the range between 1024 and 65535");
        }

        new FileStorageServer().run();
    }
}

### Файл MessageHandler.java

package com.mvnikitin.filestorage.server;

import com.mvnikitin.filestorage.common.message.AbstractNetworkMessage;
import com.mvnikitin.filestorage.common.message.file.FileAbstractCommand;
import com.mvnikitin.filestorage.common.utils.CommandProcessUtils;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

public class MessageHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg)
            throws Exception {

        AbstractNetworkMessage message = (AbstractNetworkMessage) msg;
        FileAbstractCommand cmd = null;

        try {
            if (msg instanceof FileAbstractCommand) {
                cmd = (FileAbstractCommand) msg;

                System.out.println(cmd.getRqUID());
            // TODO - писать потом Username, добавить логгер.
                System.out.println("Remote address: " +
                        ctx.channel().remoteAddress() +
                        ", Channel ID: " + ctx.channel().id().asShortText());

                cmd.setIsOnClient(false);
                CommandProcessUtils.execute(
                        cmd, ConfigSettings.getInstance().getRootDirectory());

                ctx.writeAndFlush(cmd);
            }
            else {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException ex) {
            message.setErrorText("Command of the class [" +
                    message.getClass() + "] is not supported by the server.");
            message.setResultCode(-1);
            ctx.writeAndFlush(message);
        } finally {
            ReferenceCountUtil.release(msg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}

### ConfigSettings.java

package com.mvnikitin.filestorage.server;

import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

public class ConfigSettings {
    private static final String PROPERTIES = "config.properties";

    private int port;
    private int maxTransferFileSizeMB;
    private String rootDirectory;

    private static ConfigSettings instance;

    public static ConfigSettings getInstance() throws IOException {
        if (instance == null) {
            instance = new ConfigSettings();
            instance.loadProperties();
        }
        return instance;
    }

    public int getPort() {
        return port;
    }

    public int getMaxTransferFileSizeMB() {
        return maxTransferFileSizeMB;
    }

    public String getRootDirectory() {
        return rootDirectory;
    }

    private void loadProperties() throws IOException {
        Properties properties = new Properties();
        properties.load(new FileReader("config.properties"));
        port = Integer.parseInt(properties.getProperty("port"));
        maxTransferFileSizeMB = Integer.parseInt(
                properties.getProperty("max_transfer_object_size_mb"));
        rootDirectory = properties.getProperty("root_dir");
    }
}

## Пакет com.mvnikitin.filestorage.common

### КЛАССЫ СООБЩЕНИЙ И КОМАНД

### AbstractNetworkMessage.java

package com.mvnikitin.filestorage.common.message;

import java.io.Serializable;
import java.util.UUID;

public abstract class AbstractNetworkMessage implements Serializable {

    private String rqUID;
    private int resultCode;
    private String errorText;

    public AbstractNetworkMessage() {
        rqUID = UUID.randomUUID().toString();
    }

    public abstract void dummy(); //TODO

    public String getRqUID() {
        return rqUID;
    }

    public int getResultCode() {
        return resultCode;
    }

    public String getErrorText() {
        return errorText;
    }

    public void setResultCode(int resultCode) {
        this.resultCode = resultCode;
    }

    public void setErrorText(String errorText) {
        this.errorText = errorText;
    }
}

### NotSupportedCommand.java

package com.mvnikitin.filestorage.common.message;

public class NotSupportedCommand extends AbstractNetworkMessage {

    @Override
    public void dummy() {

    }
}

### FileAbstractCommand.java

package com.mvnikitin.filestorage.common.message.file;

import com.mvnikitin.filestorage.common.message.AbstractNetworkMessage;

public abstract class FileAbstractCommand extends AbstractNetworkMessage {
    private String fileName;
    private boolean isOnClient; // Это костыль, но что делать

    public FileAbstractCommand(String fileName) {
        this.fileName = fileName;
        isOnClient = true;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public abstract void dummy();

    public boolean isOnClient() {
        return isOnClient;
    }

    public void setIsOnClient(boolean isOnClient) {
        this.isOnClient = isOnClient;
    }
}

### FileDeleteCommand.java

package com.mvnikitin.filestorage.common.message.file;

public class FileDeleteCommand extends FileAbstractCommand {

    private String fileName;
    private boolean isDeleted;

    public FileDeleteCommand(String fileName) {
        super(fileName);
        isDeleted = false;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public void setDeleted(boolean deleted) {
        isDeleted = deleted;
    }

    @Override
    public void dummy() {

    }
}

### FileDirCommand.java

package com.mvnikitin.filestorage.common.message.file;

import java.io.Serializable;
import java.util.List;

public class FileDirCommand extends FileAbstractCommand {
    private List<DirEntry> results;

    public static class DirEntry implements Serializable {
        private String entryName;
        private boolean isDirectory;

        public DirEntry(String entryName, boolean isDirectory) {
            this.entryName = entryName;
            this.isDirectory = isDirectory;
        }

        public String getEntryName() {
            return entryName;
        }

        public boolean isDirectory() {
            return isDirectory;
        }
    }

    public FileDirCommand() {
        super("");
    }

    public List<DirEntry> getResults() {
        return results;
    }

    public void setResults(List<DirEntry> results) {
        this.results = results;
    }

    @Override
    public void dummy() {
    }
}

### FileInfoCommand.java

package com.mvnikitin.filestorage.common.message.file;

import java.util.Date;

public class FileInfoCommand extends FileAbstractCommand {
    private boolean isDirectory;
    private Date creationTime;
    private Date accessedTime;
    private Date modifiedTime;
    private long size;

    public FileInfoCommand(String fileName) {
        super(fileName);
    }

    public boolean isDirectory() {
        return isDirectory;
    }

    public void setDirectory(boolean directory) {
        isDirectory = directory;
    }

    public Date getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(Date creationTime) {
        this.creationTime = creationTime;
    }

    public Date getAccessedTime() {
        return accessedTime;
    }

    public void setAccessedTime(Date accessedTime) {
        this.accessedTime = accessedTime;
    }

    public Date getModifiedTime() {
        return modifiedTime;
    }

    public void setModifiedTime(Date modifiedTime) {
        this.modifiedTime = modifiedTime;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    @Override
    public void dummy() {
    }
}

### FileRenameCommand.java

package com.mvnikitin.filestorage.common.message.file;

public class FileRenameCommand extends FileAbstractCommand {
    private String to;

    public FileRenameCommand(String from, String to) {
        super(from);
        this.to = to;
    }

    public String getFrom() {
        return super.getFileName();
    }

    public String getTo() {
        return to;
    }

    @Override
    public void dummy() {

    }
}

### FIleTransferCommand.java

package com.mvnikitin.filestorage.common.message.file;

public class FIleTransferCommand extends FileAbstractCommand {

    private int partsCount;
    private int currentPartNumber;
    private int blockSize;
    private byte[] data;
    private boolean isUpload;

    public FIleTransferCommand(String fileName, boolean isUpload) {
        super(fileName);
        this.partsCount = 0;
        this.currentPartNumber = 0;
        this.blockSize = -1;
        this.isUpload = isUpload;
        setIsOnClient(true);
    }

    public FIleTransferCommand(String fileName, boolean isUpload, int blockSize) {
        this(fileName, isUpload);
        this.blockSize = blockSize;
    }

    public int getPartsCount() {
        return partsCount;
    }

    public void setPartsCount(int partsCount) {
        this.partsCount = partsCount;
    }

    public void incrementCurrentPartNumber(){
        currentPartNumber++;
    }

    public int getCurrentPartNumber() {
        return currentPartNumber;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public int getBlockSize() {
        return blockSize;
    }

    public void setBlockSize(int blockSize) {
        this.blockSize = blockSize;
    }

    public boolean isUpload() {
        return isUpload;
    }

    @Override
    public void dummy() {

    }
}


### ОБРАБОТЧИК КОМАНД

### CommandProcessUtils.java

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


# Тестовый клиент

### TestClient.java

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
