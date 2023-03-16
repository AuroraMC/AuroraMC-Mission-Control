/*
 * Copyright (c) 2021 AuroraMC Ltd. All Rights Reserved.
 */

package net.auroramc.pathfinder.missioncontrol.backend.communication;

import net.auroramc.missioncontrol.MissionControl;

import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.UUID;
import java.util.logging.Level;

public class PathfinderCommunicationUtils {

    private static IncomingProtocolMessageThread task;

    public static void init() {
        if (task != null) {
            task.shutdown();
        }
        task = new IncomingProtocolMessageThread(35568);
        task.start();
    }

    public static UUID sendMessage(ProtocolMessage message) {
        message.setAuthenticationKey(MissionControl.getAuthKey());
        try (Socket socket = new Socket("mc2.supersecretsettings.dev", 35568)) {
            ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
            outputStream.writeObject(message);
            outputStream.flush();
            return message.getUuid();
        } catch (Exception e) {
            e.printStackTrace();
            return sendMessage(message, 1);
        }
    }
    public static UUID sendMessage(ProtocolMessage message, int level) {
        message.setAuthenticationKey(MissionControl.getAuthKey());
        try (Socket socket = new Socket("mc2.supersecretsettings.dev", 35568)) {
            ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
            outputStream.writeObject(message);
            outputStream.flush();
            return message.getUuid();
        } catch (Exception e) {
            if (level > 4) {
                MissionControl.getLogger().log(Level.WARNING, "An error occurred when attempting to contact Mission Control. Stack Trace:", e);
                return null;
            }
            return sendMessage(message, level + 1);
        }
    }

    public static void shutdown() {
        if (task != null) {
            task.shutdown();
            task = null;
        }
    }

}
