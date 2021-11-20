/*
 * Copyright (c) 2021 AuroraMC Ltd. All Rights Reserved.
 */

package net.auroramc.missioncontrol.backend.communication.panel;

import net.auroramc.missioncontrol.MissionControl;
import net.auroramc.missioncontrol.backend.communication.ProxyMessageHandler;
import net.auroramc.proxy.api.backend.communication.ProtocolMessage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.logging.Level;

public class IncomingProtocolMessageThread extends Thread {

    private int port;
    private boolean listening;
    private ServerSocket socket;

    public IncomingProtocolMessageThread(int port) {
        this.port = port;
        this.setName("Incoming Panel Protocol Messages Thread");
        this.setDaemon(true);
        listening = true;
    }

    @Override
    public void run() {
        try (ServerSocket socket = new ServerSocket(port)) {
            this.socket = socket;
            while (listening) {
                Socket connection = socket.accept();
                MissionControl.getLogger().log(Level.INFO, "Accepted connection from panel.");
                BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String message = br.readLine();
                MissionControl.getLogger().log(Level.INFO, "Command received: " + message);
                try {
                    connection.getOutputStream().write(PanelMessageHandler.onMessage(message).getBytes());
                    connection.getOutputStream().flush();
                } catch (Exception e) {
                    MissionControl.getLogger().log(Level.WARNING,"An error occurred when attempting to handle a panel message. Stack trace: ", e);
                }
            }
        } catch (SocketException e) {
            listening = false;
        } catch (IOException e) {
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
