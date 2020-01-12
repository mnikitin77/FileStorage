package com.mvnikitin.filestorage.server;

import com.mvnikitin.filestorage.server.service.ConfigSettings;
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
                                    new ClientConfigHandler(),
                                    new RegisterUserHandler(),
                                    new ServiceMessageHandler()
                            // FileMessageHandler is added to the
                            // pipeline in ServiceMessagehandler.channelRead().
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

        // Clean local application resources
            ConfigSettings.getInstance().clearResources();
        }
    }

    public static void main(String[] args) throws Exception {
        ConfigSettings cfg = ConfigSettings.getInstance();
        if (cfg == null) {
            // Error when initializing config data.
            return;
        }

        maxMessageSize = 1024 * 1024 *
                cfg.getMaxTransferFileSizeMB();

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