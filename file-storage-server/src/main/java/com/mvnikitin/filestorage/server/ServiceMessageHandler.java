package com.mvnikitin.filestorage.server;

import com.mvnikitin.filestorage.common.message.AbstractNetworkMessage;
import com.mvnikitin.filestorage.common.message.MessageType;
import com.mvnikitin.filestorage.common.message.service.LogoffCommand;
import com.mvnikitin.filestorage.common.message.service.LogonCommand;
import com.mvnikitin.filestorage.server.service.ClientSession;
import com.mvnikitin.filestorage.server.service.ServiceCommandProcessUtils;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

import java.text.SimpleDateFormat;
import java.util.Date;

public class ServiceMessageHandler extends ChannelInboundHandlerAdapter {

    private final static SimpleDateFormat DATE_FORMAT
            = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    private ClientSession client;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg)
            throws Exception {

        AbstractNetworkMessage cmd = (AbstractNetworkMessage) msg;

        try {

            if (cmd.getType() == MessageType.SERVICE) {
                // TODO - писать потом Username, добавить логгер.
                System.out.println("[" + DATE_FORMAT.format(new Date()) + "]: " +
                        ctx.channel().remoteAddress() +
                        (client != null ? (" [" + client.getUsername() + "]") :
                        "") +
                        " command: " + cmd.getClass().getSimpleName());

                if (cmd instanceof LogoffCommand) {
                    if (client == null) {
                        clientIsNotLoggedOn(ctx, cmd);
                    } else {
                        // Sending a response to the client and closing the channel.
                        ChannelFuture future = ctx.writeAndFlush(cmd);
                        future.addListener(ChannelFutureListener.CLOSE);
                        client = null;
                    }
                } else {
                    ServiceCommandProcessUtils.execute(cmd);

                    if (cmd instanceof LogonCommand &&
                            client == null &&
                            cmd.getResultCode() == 0) {
                        // The user is logged on successfully
                        LogonCommand logon = (LogonCommand) cmd;
                        client = new ClientSession(logon.getUsername());

                        // Add the file command handler to the pipeline.
                        ctx.pipeline().addLast(new FileMessageHandler(client));
                    } else if (cmd instanceof LogonCommand && client != null) {
                        // The user is already logged on.
                        cmd.setResultCode(-1);
                        cmd.setErrorText("The user is already logged on.");
                    }

                    ctx.writeAndFlush(cmd);

                }
            } else if (client != null) {
            // The user is currently logged on, passing through the message furhter.
                ctx.fireChannelRead(msg);
            } else {
            // The user is not logged on yet.
                System.out.println("[" + DATE_FORMAT.format(new Date()) + "]: " +
                        ctx.channel().remoteAddress() +
                        " command: " + cmd.getClass().getSimpleName());

                clientIsNotLoggedOn(ctx, cmd);
            }

        } catch (IllegalArgumentException ex) {
            cmd.setErrorText("Command of [" +
                    cmd.getClass() + "] is not supported by the server.");
            cmd.setResultCode(-1);
            ctx.writeAndFlush(cmd);
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

    private void clientIsNotLoggedOn(ChannelHandlerContext ctx,
                                     AbstractNetworkMessage cmd) {
        cmd.setResultCode(-1);
        cmd.setErrorText("The user is not logged on.");
        ctx.writeAndFlush(cmd);
    }
}
