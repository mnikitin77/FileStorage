package com.mvnikitin.filestorage.server;

import com.mvnikitin.filestorage.common.message.AbstractNetworkMessage;
import com.mvnikitin.filestorage.common.message.MessageType;
import com.mvnikitin.filestorage.common.message.file.FileAbstractCommand;
import com.mvnikitin.filestorage.common.utils.FileCommandProcessUtils;
import com.mvnikitin.filestorage.server.service.ConfigSettings;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

public class FileMessageHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg)
            throws Exception {

        AbstractNetworkMessage message = (AbstractNetworkMessage) msg;
        FileAbstractCommand cmd = null;

        try {
            if (message.getType() == MessageType.FILE) {
                cmd = (FileAbstractCommand) msg;

                System.out.println(cmd.getRqUID());
            // TODO - писать потом Username, добавить логгер.
                System.out.println("Remote address: " +
                        ctx.channel().remoteAddress() +
                        ", Channel ID: " + ctx.channel().id().asShortText());

                cmd.setIsOnClient(false);
                FileCommandProcessUtils.execute(
                        //cmd, ConfigSettings.getInstance().getRootDirectory());
                        cmd, ConfigSettings.getInstance());

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
