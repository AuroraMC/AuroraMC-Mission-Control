/*
 * Copyright (c) 2021 AuroraMC Ltd. All Rights Reserved.
 */

package net.auroramc.missioncontrol.backend.communication;

import net.auroramc.core.api.backend.communication.Protocol;
import net.auroramc.core.api.backend.communication.ProtocolMessage;
import net.auroramc.core.api.backend.communication.ServerCommunicationUtils;
import net.auroramc.missioncontrol.MissionControl;
import net.auroramc.missioncontrol.NetworkManager;
import net.auroramc.missioncontrol.backend.util.Game;
import net.auroramc.missioncontrol.entities.ProxyInfo;
import net.auroramc.missioncontrol.entities.ServerInfo;
import net.auroramc.proxy.api.backend.communication.ProxyCommunicationUtils;

import java.util.stream.Collectors;

public class ServerMessageHandler {

    public static void onMessage(ProtocolMessage message) {
        switch (message.getProtocol()) {
            case PLAYER_COUNT_CHANGE: {
                String[] args = message.getExtraInfo().split("\n");
                if (args.length == 2) {
                    ServerInfo.Network network = ServerInfo.Network.valueOf(args[0]);
                    Game game = Game.valueOf(args[1]);
                    String server = message.getSender();
                    if (message.getCommand().equalsIgnoreCase("join")) {
                        NetworkManager.playerJoinedServer(server, network);
                    } else {
                        NetworkManager.playerLeftServer(server, network);
                    }
                }
                break;
            }
            case VERSION_UPDATE: {
                for (ServerInfo info : MissionControl.getServers().get(ServerInfo.Network.MAIN).values().stream().filter(serverInfo -> serverInfo.getServerType().getString("type").equalsIgnoreCase("lobby")).collect(Collectors.toList())) {
                    ProtocolMessage message1 = new ProtocolMessage(message.getProtocol(), info.getName(), message.getCommand(), message.getSender(), message.getExtraInfo());
                    ServerCommunicationUtils.sendMessage(message1, info.getNetwork());
                }
                break;
            }
            case UPDATE_PLAYER_COUNT: {
                String[] args = message.getExtraInfo().split("\n");
                if (args.length == 3) {
                    int amount = Integer.parseInt(args[0]);
                    ServerInfo.Network network = ServerInfo.Network.valueOf(args[1]);
                    String server = message.getSender();
                    NetworkManager.reportServerTotal(server, amount, network);
                }
                break;
            }
            case SHUTDOWN: {
                //This is a restart initiated by the server.
                ServerInfo.Network network = ServerInfo.Network.valueOf(message.getExtraInfo());
                ServerInfo info = MissionControl.getServers().get(network).get(message.getCommand());
                info.setStatus(ServerInfo.ServerStatus.PENDING_RESTART);

                ProtocolMessage protocolMessage = new ProtocolMessage(Protocol.EMERGENCY_SHUTDOWN, info.getName(), "restart", "Mission Control", "");
                ServerCommunicationUtils.sendMessage(protocolMessage, network);
                break;
            }
            case CONFIRM_SHUTDOWN: {
                ServerInfo.Network network = ServerInfo.Network.valueOf(message.getExtraInfo());
                ServerInfo info = MissionControl.getServers().get(network).get(message.getSender());
                if (NetworkManager.isUpdate()) {
                    NetworkManager.getRestarterThread().serverCloseConfirm(info);
                } else {
                    if (message.getCommand().equalsIgnoreCase("restart")) {
                        MissionControl.getPanelManager().closeServer(info.getName(), network);
                        MissionControl.getPanelManager().updateServer(info);
                        MissionControl.getPanelManager().openServer(info.getName(), network);
                    } else if (!message.getCommand().equalsIgnoreCase("forced")) {
                        if (info.getStatus() == ServerInfo.ServerStatus.ONLINE) {
                            NetworkManager.closeServer(info);
                            if (network == ServerInfo.Network.ALPHA) {
                                if (NetworkManager.isAlphaEnabled() && NetworkManager.isServerMonitoringEnabled(ServerInfo.Network.ALPHA)) {
                                    NetworkManager.getAlphaMonitorRunnable().serverConfirmClose(info);
                                }
                            } else if (network == ServerInfo.Network.MAIN) {
                                if (NetworkManager.isServerMonitoringEnabled(ServerInfo.Network.MAIN)) {
                                    NetworkManager.getMonitorRunnable().serverConfirmClose(info);
                                }
                            }
                        }
                    } else {
                        info.setStatus(ServerInfo.ServerStatus.RESTARTING);
                    }
                }
                break;
            }
            case SERVER_ONLINE: {
                ServerInfo.Network network = ServerInfo.Network.valueOf(message.getExtraInfo());
                ServerInfo info = MissionControl.getServers().get(network).get(message.getSender());
                if (NetworkManager.isUpdate()) {
                    NetworkManager.reportServerTotal(info.getName(), 0, network);
                    NetworkManager.getRestarterThread().serverStartConfirm(info);
                } else {
                    if (info.getStatus() == ServerInfo.ServerStatus.STARTING) {
                        NetworkManager.reportServerTotal(info.getName(), 0, network);
                        info.setStatus(ServerInfo.ServerStatus.ONLINE);
                        NetworkManager.serverOpenConfirmation(info);
                    } else if (info.getStatus() != ServerInfo.ServerStatus.ONLINE) {
                        NetworkManager.reportServerTotal(info.getName(), 0, network);
                        info.setStatus(ServerInfo.ServerStatus.ONLINE);
                    }
                }
                break;
            }
        }
    }

}
