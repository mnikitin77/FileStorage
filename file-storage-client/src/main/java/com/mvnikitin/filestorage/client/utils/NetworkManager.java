package com.mvnikitin.filestorage.client.utils;

import com.mvnikitin.filestorage.common.message.AbstractNetworkMessage;
import io.netty.handler.codec.serialization.ObjectDecoderInputStream;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;

import java.io.IOException;
import java.net.Socket;

public class NetworkManager {
    private static int MAX_OBJ_SIZE_MB = 50;

    private static Socket socket;
    private static ObjectEncoderOutputStream out;
    private static ObjectDecoderInputStream in;

    public static void start(String host, int port) throws IOException {
        socket = new Socket(host, port);
        out = new ObjectEncoderOutputStream(socket.getOutputStream());
        in = new ObjectDecoderInputStream(socket.getInputStream(),
                MAX_OBJ_SIZE_MB * 1024 * 1024);
    }

    public static void stop() {
        try {
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean sendMsg(AbstractNetworkMessage msg) {
        try {
            out.writeObject(msg);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static AbstractNetworkMessage readObject() throws ClassNotFoundException, IOException {
        Object obj = in.readObject();
        return (AbstractNetworkMessage) obj;
    }
}