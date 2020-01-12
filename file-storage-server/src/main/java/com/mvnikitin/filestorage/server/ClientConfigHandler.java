package com.mvnikitin.filestorage.server;

import com.mvnikitin.filestorage.common.message.AbstractNetworkMessage;
import com.mvnikitin.filestorage.common.message.service.GetConfigInfoCommand;
import com.mvnikitin.filestorage.server.service.ConfigSettings;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

import java.text.DateFormat;
import java.util.Date;

public class ClientConfigHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg)
            throws Exception {

        AbstractNetworkMessage cmd = (AbstractNetworkMessage) msg;

        try {
            if (cmd instanceof GetConfigInfoCommand) {
                System.out.println("[" +
                        DateFormat.getDateTimeInstance().format(new Date()) +
                        "]: " + ctx.channel().remoteAddress() +
                        " command: " + cmd.getClass().getSimpleName());

                ((GetConfigInfoCommand)cmd).setBlockSize(
                        ConfigSettings.getInstance().getBlockSize());
                cmd.setResultCode(0);

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
