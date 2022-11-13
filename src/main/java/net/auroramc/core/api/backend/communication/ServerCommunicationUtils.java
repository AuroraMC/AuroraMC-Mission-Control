/*
 * Copyright (c) 2021 AuroraMC Ltd. All Rights Reserved.
 */

package net.auroramc.core.api.backend.communication;

import net.auroramc.missioncontrol.MissionControl;
import net.auroramc.missioncontrol.NetworkManager;
import net.auroramc.missioncontrol.entities.ServerInfo;

import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.UUID;
import java.util.logging.Level;

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
            message.setServer(info.getName());
            message.setAuthenticationKey(info.getAuthKey());
            message.setNetwork(info.getNetwork().name());
            MissionControl.getLogger().log(Level.FINEST, "Sending protocol message to " + info.getName() + " under protocol " + message.getProtocol().name());
            try (Socket socket = new Socket(info.getIp(), info.getProtocolPort())) {
                ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
                outputStream.writeObject(message);
                outputStream.flush();
                info.ping();
                return message.getUuid();
            } catch (Exception e) {
                if (message.getProtocol() != Protocol.UPDATE_PLAYER_COUNT || !NetworkManager.isUpdate()) {
                    return sendMessage(message, network, 1);
                }
                return null;
            }
        }
        return null;
    }
    public static UUID sendMessage(ProtocolMessage message, ServerInfo.Network network, int level) {
        ServerInfo info = MissionControl.getServers().get(network).get(message.getDestination());
        if (info != null) {
            message.setServer(info.getName());
            message.setAuthenticationKey(info.getAuthKey());
            message.setNetwork(info.getNetwork().name());
            try (Socket socket = new Socket(info.getIp(), info.getProtocolPort())) {
                ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
                outputStream.writeObject(message);
                outputStream.flush();
                info.ping();
                return message.getUuid();
            } catch (Exception e) {
                if (message.getProtocol() != Protocol.UPDATE_PLAYER_COUNT || !NetworkManager.isUpdate()) {
                    if (level > 4) {
                        if (System.currentTimeMillis() - info.getLastPing() > 300000) {
                            //Restart server as it has been unresponsive for at least the past 5 minutes.
                            MissionControl.getLogger().log(Level.WARNING, "Last successful ping to server " + info.getName() + " on network " + info.getNetwork().name() + " was over 5 minutes ago, restarting server. Stack Trace:", e);
                            MissionControl.getPanelManager().updateServer(info);
                        } else {
                            MissionControl.getLogger().log(Level.WARNING, "An error occurred when attempting to contact server " + info.getName() + " on network " + info.getNetwork().name() + ". Stack Trace:", e);
                        }
                        return null;
                    }
                    return sendMessage(message, network, level + 1);
                }
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
