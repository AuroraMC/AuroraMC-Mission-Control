package net.auroramc.proxy.api.backend.communication;

import net.auroramc.missioncontrol.backend.communication.ProxyMessageHandler;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

public class IncomingProtocolMessageThread extends Thread {

    private int port;
    private boolean listening;
    private ServerSocket socket;

    public IncomingProtocolMessageThread(int port) {
        this.port = port;
        this.setName("Incoming Proxy Protocol Messages Thread");
        this.setDaemon(true);
    }

    @Override
    public void run() {
        try (ServerSocket socket = new ServerSocket(port)) {
            this.socket = socket;
            while (listening) {
                Socket connection = socket.accept();
                ObjectInputStream objectInputStream = (ObjectInputStream) connection.getInputStream();
                ProtocolMessage message = (ProtocolMessage) objectInputStream.readObject();
                ProxyMessageHandler.onMessage(message);
            }
        } catch (SocketException e) {
            listening = false;
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void shutdown() {
        listening = false;
        try {
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
