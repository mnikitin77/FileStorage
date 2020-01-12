package com.mvnikitin.filestorage.server;

import com.mvnikitin.filestorage.common.message.AbstractNetworkMessage;
import com.mvnikitin.filestorage.common.message.MessageType;
import com.mvnikitin.filestorage.common.message.file.FileAbstractCommand;
import com.mvnikitin.filestorage.common.utils.FileCommandProcessUtils;
import com.mvnikitin.filestorage.server.service.ClientSession;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

import java.text.DateFormat;
import java.util.Date;

public class FileMessageHandler extends ChannelInboundHandlerAdapter {

    private ClientSession client;

    public FileMessageHandler (ClientSession client) {
        this.client = client;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg)
            throws Exception {

        AbstractNetworkMessage message = (AbstractNetworkMessage) msg;
        FileAbstractCommand cmd = null;

        try {
            if (message.getType() == MessageType.FILE) {
                cmd = (FileAbstractCommand) msg;

                System.out.println("[" +
                        DateFormat.getDateTimeInstance().format(new Date()) +
                        "]: " + ctx.channel().remoteAddress() +
                        " [" + client.getUsername() + "] command: " +
                        cmd.getClass().getSimpleName());

                cmd.setIsOnClient(false);
                FileCommandProcessUtils.execute(cmd, client);

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
