package net.auroramc.missioncontrol.backend.communication;

import net.auroramc.core.api.backend.communication.ProtocolMessage;
import net.auroramc.missioncontrol.MissionControl;
import net.auroramc.missioncontrol.NetworkManager;
import net.auroramc.missioncontrol.NetworkMonitorRunnable;
import net.auroramc.missioncontrol.NetworkRestarterThread;
import net.auroramc.missioncontrol.backend.Game;
import net.auroramc.missioncontrol.entities.ProxyInfo;
import net.auroramc.missioncontrol.entities.ServerInfo;

import java.util.UUID;

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
                        NetworkManager.playerJoinedServer(server, game, network);
                    } else {
                        NetworkManager.playerLeftServer(server, game, network);
                    }
                }
                break;
            }
            case UPDATE_PLAYER_COUNT: {
                String[] args = message.getExtraInfo().split("\n");
                if (args.length == 3) {
                    int amount = Integer.parseInt(args[0]);
                    ServerInfo.Network network = ServerInfo.Network.valueOf(args[1]);
                    Game game = Game.valueOf(args[2]);
                    String server = message.getSender();
                    NetworkManager.reportServerTotal(server, game, amount, network);
                }
                break;
            }
            case SHUTDOWN: {
                //This is a restart initiated by the server.
                ServerInfo.Network network = ServerInfo.Network.valueOf(message.getExtraInfo());
                ServerInfo info = MissionControl.getServers().get(network).get(message.getSender());
                MissionControl.getPanelManager().closeServer(info.getName(), network);
                MissionControl.getPanelManager().openServer(info.getName(), network);
            }
            case CONFIRM_SHUTDOWN: {
                ServerInfo.Network network = ServerInfo.Network.valueOf(message.getExtraInfo());
                ServerInfo info = MissionControl.getServers().get(network).get(message.getSender());
                NetworkManager.serverClose(info.getName(), Game.valueOf(info.getServerType().getString("game")), network);
                if (NetworkManager.isUpdate()) {
                    NetworkManager.getRestarterThread().serverCloseConfirm(info);
                } else {
                    NetworkManager.closeServer(info);
                    if (message.getCommand().equalsIgnoreCase("restart")) {
                        MissionControl.getPanelManager().closeServer(info.getName(), network);
                        MissionControl.getPanelManager().updateServer(info);
                        MissionControl.getPanelManager().openServer(info.getName(), network);
                    } else {
                        if (network == ServerInfo.Network.ALPHA) {
                            NetworkManager.getAlphaMonitorRunnable().serverConfirmClose(info);
                        } else if (network == ServerInfo.Network.MAIN) {
                            NetworkManager.getMonitorRunnable().serverConfirmClose(info);
                        }
                    }
                }
                break;
            }
            case SERVER_ONLINE: {
                ServerInfo.Network network = ServerInfo.Network.valueOf(message.getExtraInfo());
                ServerInfo info = MissionControl.getServers().get(network).get(message.getSender());
                NetworkManager.reportServerTotal(info.getName(), Game.valueOf(info.getServerType().getString("game")), 0, network);
                if (NetworkManager.isUpdate()) {
                    NetworkManager.getRestarterThread().serverStartConfirm(info);
                } else {
                    NetworkManager.serverOpenConfirmation(info);
                }
                break;
            }
        }
    }

}
