/*
 * Copyright (c) 2021 AuroraMC Ltd. All Rights Reserved.
 */

package net.auroramc.core.api.backend.communication;

import net.auroramc.missioncontrol.MissionControl;
import net.auroramc.missioncontrol.entities.ServerInfo;

import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.UUID;

public class ServerCommunicationUtils {

    private static IncomingProtocolMessageThread task;

    public static void init() {
        if (task != null) {
            task.shutdown();
        }
        task = new IncomingProtocolMessageThread(35565);
        task.start();
    }

    public static UUID sendMessage(ProtocolMessage message, ServerInfo.Network network) {
       ServerInfo info = MissionControl.getServers().get(network).get(message.getDestination());
        if (info != null) {
            try (Socket socket = new Socket(info.getIp(), info.getProtocolPort())) {
                ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
                outputStream.writeObject(message);
                outputStream.flush();
                return message.getUuid();
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
        return null;
    }

    public static void shutdown() {
        if (task != null) {
            task.shutdown();
            task = null;
        }
    }

}
