package com.mvnikitin.filestorage.server;

import com.mvnikitin.filestorage.common.message.AbstractNetworkMessage;
import com.mvnikitin.filestorage.common.message.MessageType;
import com.mvnikitin.filestorage.server.service.ServiceCommandProcessUtils;
import com.mvnikitin.filestorage.server.service.ConfigSettings;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

public class ServiceMessageHandler extends ChannelInboundHandlerAdapter{

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg)
            throws Exception {

        AbstractNetworkMessage cmd = (AbstractNetworkMessage) msg;

        try {
            if (cmd.getType() == MessageType.SERVICE) {
                System.out.println(cmd.getRqUID());
                // TODO - писать потом Username, добавить логгер.
                System.out.println("Remote address: " +
                        ctx.channel().remoteAddress() +
                        ", Channel ID: " + ctx.channel().id().asShortText());

                ServiceCommandProcessUtils.execute(
                        cmd, ConfigSettings.getInstance());

                ctx.writeAndFlush(cmd);
            } else {
                ctx.fireChannelRead(msg);
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
}
