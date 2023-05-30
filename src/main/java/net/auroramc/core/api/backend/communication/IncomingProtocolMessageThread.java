/*
 * Copyright (c) 2021-2023 AuroraMC Ltd. All Rights Reserved.
 *
 * PRIVATE AND CONFIDENTIAL - Distribution and usage outside the scope of your job description is explicitly forbidden except in circumstances where a company director has expressly given written permission to do so.
 */

package net.auroramc.core.api.backend.communication;

import net.auroramc.missioncontrol.MissionControl;
import net.auroramc.missioncontrol.backend.communication.ServerMessageHandler;
import net.auroramc.missioncontrol.entities.ServerInfo;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.StreamCorruptedException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Level;

public class IncomingProtocolMessageThread extends Thread {

    private static final ScheduledExecutorService scheduler;

    static {
        scheduler = Executors.newScheduledThreadPool(100);
    }

    private int port;
    private boolean listening;
    private ServerSocket socket;

    public IncomingProtocolMessageThread(int port) {

        this.port = port;
        this.setName("Incoming Server Protocol Messages Thread");
        this.setDaemon(true);
        listening = true;
    }

    @Override
    public void run() {
        try (ServerSocket socket = new ServerSocket(port)) {
            this.socket = socket;
            while (listening) {
                try (Socket connection = socket.accept()) {
                    ObjectInputStream objectInputStream = new ObjectInputStream(connection.getInputStream());
                    ProtocolMessage message = (ProtocolMessage) objectInputStream.readObject();
                    connection.close();
                    if (!message.getAuthenticationKey().equals(MissionControl.getServers().get(ServerInfo.Network.valueOf(message.getNetwork())).get(message.getServer()).getAuthKey())) {
                        //Check if the auth keys match.
                        return;
                    }
                    MissionControl.getLogger().log(Level.FINEST, "Accepted connection from server " + message.getSender() + " under protocol " + message.getProtocol().name() + ".");
                    scheduler.execute(() -> {
                        try {
                            ServerMessageHandler.onMessage(message);
                        } catch (Exception e) {
                            MissionControl.getLogger().log(Level.WARNING,"An error occurred when attempting to handle a server message. Stack trace: ", e);
                        }
                    });
                } catch (StreamCorruptedException e) {
                    e.printStackTrace();
                }
            }
        } catch (SocketException e) {
            listening = false;
        } catch (Exception e) {
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
