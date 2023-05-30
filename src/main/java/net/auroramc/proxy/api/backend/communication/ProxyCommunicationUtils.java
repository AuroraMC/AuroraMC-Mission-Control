/*
 * Copyright (c) 2021-2023 AuroraMC Ltd. All Rights Reserved.
 *
 * PRIVATE AND CONFIDENTIAL - Distribution and usage outside the scope of your job description is explicitly forbidden except in circumstances where a company director has expressly given written permission to do so.
 */

package net.auroramc.proxy.api.backend.communication;

import net.auroramc.missioncontrol.MissionControl;
import net.auroramc.missioncontrol.NetworkManager;
import net.auroramc.missioncontrol.entities.ProxyInfo;
import net.auroramc.missioncontrol.entities.ServerInfo;

import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.UUID;
import java.util.logging.Level;

public class ProxyCommunicationUtils {

    private static IncomingProtocolMessageThread task;

    public static void init() {
        if (task != null) {
            task.shutdown();
        }
        task = new IncomingProtocolMessageThread(35566);
        task.start();
    }

    public static UUID sendMessage(ProtocolMessage message) {
        ProxyInfo info = MissionControl.getProxies().get(UUID.fromString(message.getDestination()));
        if (info != null) {
            message.setProxy(info.getUuid());
            message.setAuthenticationKey(info.getAuthKey());
            message.setNetwork(info.getNetwork().name());
            MissionControl.getLogger().log(Level.FINEST, "Sending protocol message to " + message.getProxy().toString() + " under protocol " + message.getProtocol().name());
            try (Socket socket = new Socket(info.getIp(), info.getProtocolPort())) {
                ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
                outputStream.writeObject(message);
                outputStream.flush();
                info.ping();
                return message.getUuid();
            } catch (Exception e) {
                if (message.getProtocol() != Protocol.UPDATE_PLAYER_COUNT || !NetworkManager.isProxyUpdate()) {
                    return sendMessage(message, 1);
                }
                return null;
            }
        }
        return null;
    }

    public static UUID sendMessage(ProtocolMessage message, int level) {
        ProxyInfo info = MissionControl.getProxies().get(UUID.fromString(message.getDestination()));
        if (info != null) {
            message.setProxy(info.getUuid());
            message.setAuthenticationKey(info.getAuthKey());
            message.setNetwork(info.getNetwork().name());
            MissionControl.getLogger().log(Level.FINEST, "Sending protocol message to " + info.getIp() + ":" + info.getProtocolPort());
            try (Socket socket = new Socket(info.getIp(), info.getProtocolPort())) {
                ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
                outputStream.writeObject(message);
                outputStream.flush();
                info.ping();
                return message.getUuid();
            } catch (Exception e) {
                if (message.getProtocol() != Protocol.UPDATE_PLAYER_COUNT || !NetworkManager.isProxyUpdate()) {
                    if (level > 4) {
                        if (System.currentTimeMillis() - info.getLastPing() > 300000 && info.getNetwork() != ServerInfo.Network.TEST) {
                            MissionControl.getLogger().log(Level.WARNING, "Last successful ping to proxy " + info.getUuid().toString() + " on network " + info.getNetwork().name() + " was over 5 minutes ago, restarting proxy. Stack Trace:", e);
                            MissionControl.getPanelManager().updateProxy(info);
                        } else {
                            MissionControl.getLogger().log(Level.WARNING, "An error occurred when attempting to contact proxy " + info.getUuid().toString() + " on network " + info.getNetwork().name() + ". Stack Trace:", e);
                        }
                        return null;
                    }
                    return sendMessage(message, level + 1);
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
