/*
 * Copyright (c) 2021-2023 AuroraMC Ltd. All Rights Reserved.
 *
 * PRIVATE AND CONFIDENTIAL - Distribution and usage outside the scope of your job description is explicitly forbidden except in circumstances where a company director has expressly given written permission to do so.
 */

package net.auroramc.missioncontrol.backend.communication;

import net.auroramc.core.api.backend.communication.Protocol;
import net.auroramc.core.api.backend.communication.ServerCommunicationUtils;
import net.auroramc.missioncontrol.MissionControl;
import net.auroramc.missioncontrol.NetworkManager;
import net.auroramc.missioncontrol.NetworkRestarterThread;
import net.auroramc.missioncontrol.backend.util.MaintenanceMode;
import net.auroramc.missioncontrol.entities.ProxyInfo;
import net.auroramc.missioncontrol.entities.ServerInfo;
import net.auroramc.proxy.api.backend.communication.ProtocolMessage;
import net.auroramc.proxy.api.backend.communication.ProxyCommunicationUtils;

import java.util.UUID;
import java.util.stream.Collectors;

public class ProxyMessageHandler {

    public static void onMessage(ProtocolMessage message) {
        switch (message.getProtocol()) {
            case UPDATE_MOTD: {
                String[] args = message.getExtraInfo().split("\n");
                if (args.length == 2) {
                    String motd = args[0];
                    ServerInfo.Network network = ServerInfo.Network.valueOf(args[1]);
                    if (message.getCommand().equalsIgnoreCase("maintenance")) {
                        NetworkManager.setMaintenanceMotd(network, motd);
                        MissionControl.getDbManager().changeMaintenanceMotd(network, motd);
                    } else {
                        NetworkManager.setMotd(network, motd);
                        MissionControl.getDbManager().changeMotd(network, motd);
                    }

                }
            }
            case MEDIA_RANK_JOIN_LEAVE:
            case STAFF_RANK_JOIN_LEAVE:
            case UPDATE_CHAT_SLOW:
            case UPDATE_CHAT_SILENCE:
            case GLOBAL_MESSAGE:
            case APPROVAL_NOTIFICATION:
            case ANNOUNCE: {
                String[] args = message.getExtraInfo().split("\n");
                for (ProxyInfo info : MissionControl.getProxies().values().stream().filter(proxyInfo -> proxyInfo.getNetwork() == ServerInfo.Network.valueOf(args[1])).collect(Collectors.toList())) {
                    ProtocolMessage message1 = new ProtocolMessage(message.getProtocol(), info.getUuid().toString(), message.getCommand(), message.getSender(), args[0]);
                    ProxyCommunicationUtils.sendMessage(message1);
                }
                break;
            }
            case UPDATE_MAINTENANCE_MODE: {
                String[] args = message.getExtraInfo().split("\n");
                switch (message.getCommand().toLowerCase()) {
                    case "enable": {
                        MissionControl.getDbManager().changeMaintenance(ServerInfo.Network.valueOf(args[1]), true);
                        NetworkManager.setMaintenance(ServerInfo.Network.valueOf(args[1]), true);
                        MissionControl.getDbManager().changeMaintenanceMode(ServerInfo.Network.valueOf(args[1]),  MaintenanceMode.valueOf(args[0]));
                        NetworkManager.setMaintenanceMode(ServerInfo.Network.valueOf(args[1]), MaintenanceMode.valueOf(args[0]));
                        break;
                    }
                    case "disable": {
                        MissionControl.getDbManager().changeMaintenance(ServerInfo.Network.valueOf(args[1]), false);
                        NetworkManager.setMaintenance(ServerInfo.Network.valueOf(args[1]), false);
                        break;
                    }
                    case "update": {
                        MissionControl.getDbManager().changeMaintenanceMode(ServerInfo.Network.valueOf(args[1]),  MaintenanceMode.valueOf(args[0]));
                        NetworkManager.setMaintenanceMode(ServerInfo.Network.valueOf(args[1]), MaintenanceMode.valueOf(args[0]));
                        break;
                    }
                }
                for (ProxyInfo info : MissionControl.getProxies().values().stream().filter(proxyInfo -> proxyInfo.getNetwork() == ServerInfo.Network.valueOf(args[1])).collect(Collectors.toList())) {
                    ProtocolMessage message1 = new ProtocolMessage(message.getProtocol(), info.getUuid().toString(), message.getCommand(), message.getSender(), args[0]);
                    ProxyCommunicationUtils.sendMessage(message1);
                }
                break;
            }
            case UPDATE_PLAYER_COUNT: {
                String[] args = message.getExtraInfo().split("\n");
                if (args.length == 2) {
                    NetworkManager.reportProxyTotal(UUID.fromString(message.getSender()), Integer.parseInt(args[0]));
                }
                break;
            }
            case PROXY_ONLINE: {
                NetworkManager.reportProxyTotal(UUID.fromString(message.getSender()), 0);
                ProxyInfo info = MissionControl.getProxies().get(UUID.fromString(message.getSender()));
                info.setStatus(ProxyInfo.ProxyStatus.ONLINE);
                MissionControl.getProxyManager().addServer(info);
                if (NetworkManager.isProxyUpdate() && NetworkManager.getProxyRestarterThread().getNetwork() == info.getNetwork()) {
                    NetworkManager.getProxyRestarterThread().proxyStartConfirm(MissionControl.getProxies().get(UUID.fromString(message.getSender())));
                } else {
                    if (info.getStatus() == ProxyInfo.ProxyStatus.STARTING) {
                        NetworkManager.proxyOpenConfirmation(MissionControl.getProxies().get(UUID.fromString(message.getSender())));
                    }
                }
                break;
            }
            case SHUTDOWN: {
                //This is a restart initiated by the proxy.
                ProxyInfo info = MissionControl.getProxies().get(UUID.fromString(message.getCommand()));
                info.setStatus(ProxyInfo.ProxyStatus.PENDING_RESTART);

                net.auroramc.proxy.api.backend.communication.ProtocolMessage protocolMessage = new net.auroramc.proxy.api.backend.communication.ProtocolMessage(net.auroramc.proxy.api.backend.communication.Protocol.EMERGENCY_SHUTDOWN, info.getUuid().toString(), "restart", "Mission Control", "");
                ProxyCommunicationUtils.sendMessage(protocolMessage);
                break;
            }
            case CONFIRM_SHUTDOWN: {
                ProxyInfo info = MissionControl.getProxies().get(UUID.fromString(message.getSender()));
                if (NetworkManager.isProxyUpdate() && NetworkManager.getProxyRestarterThread().getNetwork() == info.getNetwork()) {
                    NetworkManager.getProxyRestarterThread().proxyCloseConfirm(info);
                } else {
                    ServerInfo.Network network = ServerInfo.Network.valueOf(message.getExtraInfo());
                    if (message.getCommand().equalsIgnoreCase("restart")) {
                        MissionControl.getPanelManager().closeServer(info.getUuid().toString(), network);
                        MissionControl.getPanelManager().updateProxy(info);
                        MissionControl.getPanelManager().openServer(info.getUuid().toString(), network);
                        info.setStatus(ProxyInfo.ProxyStatus.RESTARTING);
                        new Thread(NetworkManager::waitForProxyResponse).start();
                    } else if (message.getCommand().equalsIgnoreCase("close")) {
                        NetworkManager.deleteProxy(info);
                        if (network == ServerInfo.Network.ALPHA && NetworkManager.isServerMonitoringEnabled(ServerInfo.Network.ALPHA)) {
                            NetworkManager.getAlphaMonitorRunnable().proxyConfirmClose(info);
                        } else if (network == ServerInfo.Network.MAIN && NetworkManager.isServerMonitoringEnabled(ServerInfo.Network.MAIN)) {
                            NetworkManager.getMonitorRunnable().proxyConfirmClose(info);
                        }
                    } else if (message.getCommand().equalsIgnoreCase("forced")) {
                        info.setStatus(ProxyInfo.ProxyStatus.RESTARTING);
                    }

                }
                break;
            }
            case PLAYER_COUNT_CHANGE: {
                ServerInfo.Network network = ServerInfo.Network.valueOf(message.getExtraInfo());
                UUID uuid = UUID.fromString(message.getSender());
                if (message.getCommand().equalsIgnoreCase("join")) {
                    NetworkManager.playerJoinedNetwork(uuid);
                } else {
                    NetworkManager.playerLeftNetwork(uuid);
                }
                break;
            }
        }
    }

}
