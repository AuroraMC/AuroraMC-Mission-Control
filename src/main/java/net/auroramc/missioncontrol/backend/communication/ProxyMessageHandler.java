package net.auroramc.missioncontrol.backend.communication;

import net.auroramc.missioncontrol.MissionControl;
import net.auroramc.missioncontrol.NetworkManager;
import net.auroramc.missioncontrol.NetworkMonitorRunnable;
import net.auroramc.missioncontrol.NetworkRestarterThread;
import net.auroramc.missioncontrol.entities.ProxyInfo;
import net.auroramc.missioncontrol.entities.ServerInfo;
import net.auroramc.proxy.api.backend.communication.Protocol;
import net.auroramc.proxy.api.backend.communication.ProtocolMessage;
import net.auroramc.proxy.api.backend.communication.ProxyCommunicationUtils;

import java.util.UUID;
import java.util.stream.Collectors;

public class ProxyMessageHandler {

    public static void onMessage(ProtocolMessage message) {
        switch (message.getProtocol()) {
            case MEDIA_RANK_JOIN_LEAVE:
            case STAFF_RANK_JOIN_LEAVE:
            case UPDATE_MOTD:
            case UPDATE_CHAT_SLOW:
            case UPDATE_CHAT_SILENCE:
            case UPDATE_MAINTENANCE_MODE:
            case GLOBAL_MESSAGE:
            case ANNOUNCE: {
                String[] args = message.getExtraInfo().split("\n");
                for (ProxyInfo info : MissionControl.getProxies().values().stream().filter(proxyInfo -> proxyInfo.getNetwork() == ServerInfo.Network.valueOf(args[1])).collect(Collectors.toList())) {
                    ProtocolMessage message1 = new ProtocolMessage(message.getProtocol(), info.getUuid().toString(), message.getCommand(), message.getSender(), args[0]);
                    ProxyCommunicationUtils.sendMessage(message1);
                }
                break;
            }
            case UPDATE_PLAYER_COUNT: {
                String[] args = message.getExtraInfo().split("\n");
                if (args.length == 2) {
                    NetworkManager.reportProxyTotal(UUID.fromString(message.getSender()), Integer.parseInt(args[0]), ServerInfo.Network.valueOf(args[1]));
                }
                break;
            }
            case PROXY_ONLINE: {
                if (NetworkMonitorRunnable.isUpdate()) {
                    if (NetworkManager.getRestarterThread().getProxyRestartMode() == NetworkRestarterThread.RestartMode.SOLO) {
                        NetworkManager.getRestarterThread().proxyStartConfirm(MissionControl.getProxies().get(UUID.fromString(message.getSender())));
                    } else {
                        NetworkManager.proxyOpenConfirmation(MissionControl.getProxies().get(UUID.fromString(message.getSender())));
                    }
                } else {
                    NetworkManager.proxyOpenConfirmation(MissionControl.getProxies().get(UUID.fromString(message.getSender())));
                }
                break;
            }
            case CONFIRM_SHUTDOWN: {
                if (NetworkMonitorRunnable.isUpdate()) {
                    NetworkManager.getRestarterThread().proxyCloseConfirm(MissionControl.getProxies().get(UUID.fromString(message.getSender())));
                } else {
                    ServerInfo.Network network = ServerInfo.Network.valueOf(message.getExtraInfo());
                    ProxyInfo info = MissionControl.getProxies().get(UUID.fromString(message.getSender()));
                    NetworkManager.deleteProxy(MissionControl.getProxies().get(UUID.fromString(message.getSender())));
                    if (network == ServerInfo.Network.ALPHA) {
                        NetworkManager.getAlphaMonitorRunnable().proxyConfirmClose(info);
                    } else if (network == ServerInfo.Network.MAIN) {
                        NetworkManager.getMonitorRunnable().proxyConfirmClose(info);
                    }
                }
                break;
            }
            case PLAYER_COUNT_CHANGE: {
                ServerInfo.Network network = ServerInfo.Network.valueOf(message.getExtraInfo());
                UUID uuid = UUID.fromString(message.getSender());
                if (message.getCommand().equalsIgnoreCase("join")) {
                    NetworkManager.playerJoinedNetwork(uuid, network);
                } else {
                    NetworkManager.playerLeftNetwork(uuid, network);
                }
                break;
            }
        }
    }

}
