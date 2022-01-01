/*
 * Copyright (c) 2021 AuroraMC Ltd. All Rights Reserved.
 */

package net.auroramc.missioncontrol.backend.communication;

import net.auroramc.core.api.backend.communication.ProtocolMessage;
import net.auroramc.missioncontrol.MissionControl;
import net.auroramc.missioncontrol.NetworkManager;
import net.auroramc.missioncontrol.backend.util.Game;
import net.auroramc.missioncontrol.entities.ServerInfo;

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
                ServerInfo info = MissionControl.getServers().get(network).get(message.getSender());
                info.setStatus(ServerInfo.ServerStatus.RESTARTING);
                MissionControl.getPanelManager().closeServer(info.getName(), network);
                MissionControl.getPanelManager().openServer(info.getName(), network);
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
                    } else if (!message.getCommand().equalsIgnoreCase("force")) {
                        if (info.getStatus() == ServerInfo.ServerStatus.ONLINE) {
                            NetworkManager.closeServer(info);
                            if (network == ServerInfo.Network.ALPHA) {
                                NetworkManager.getAlphaMonitorRunnable().serverConfirmClose(info);
                            } else if (network == ServerInfo.Network.MAIN) {
                                NetworkManager.getMonitorRunnable().serverConfirmClose(info);
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
                NetworkManager.reportServerTotal(info.getName(), 0, network);
                if (NetworkManager.isUpdate()) {
                    NetworkManager.getRestarterThread().serverStartConfirm(info);
                } else {
                    if (info.getStatus() == ServerInfo.ServerStatus.STARTING) {
                        NetworkManager.serverOpenConfirmation(info);
                    }
                }
                break;
            }
        }
    }

}
