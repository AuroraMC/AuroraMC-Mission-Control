package net.auroramc.missioncontrol;

import net.auroramc.core.api.backend.communication.ServerCommunicationUtils;
import net.auroramc.missioncontrol.backend.Game;
import net.auroramc.missioncontrol.entities.ProxyInfo;
import net.auroramc.missioncontrol.entities.ServerInfo;
import net.auroramc.proxy.api.backend.communication.Protocol;
import net.auroramc.proxy.api.backend.communication.ProtocolMessage;
import net.auroramc.proxy.api.backend.communication.ProxyCommunicationUtils;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class NetworkMonitorRunnable implements Runnable {

    private static boolean update;
    private final Logger logger;
    private final ServerInfo.Network network;
    private final List<ServerInfo> serversPendingRestart;
    private final List<ProxyInfo> proxiesPendingRestart;


    public NetworkMonitorRunnable(Logger logger, ServerInfo.Network network) {
        this.logger = logger;
        update = false;
        this.network = network;
        serversPendingRestart = new ArrayList<>();
        proxiesPendingRestart = new ArrayList<>();
    }

    @Override
    public void run() {
        //If an update is not in progress, check player counts.
        if (!update) {
            //Check to see if there are sufficient/too many connection nodes open. Open/close as many as are needed. Keep open as many connection nodes are needed to support all the players + 1. (or +2 if there is no-one online)
            if (NetworkManager.getNetworkPlayerTotal().get(network) > 0) {
                List<UUID> uuids = MissionControl.getProxies().keySet().stream().filter(uuid -> MissionControl.getProxies().get(uuid).getNetwork() == network).collect(Collectors.toList());
                int totalProxiesNeeded = (NetworkManager.getNetworkPlayerTotal().get(network) / 200) + ((NetworkManager.getNetworkPlayerTotal().get(network) % 200 > 0)?1:0) + 1;
                if (uuids.size() - proxiesPendingRestart.size() != totalProxiesNeeded) {
                    //There are not enough/too many proxies open. Close/open some.
                    int i = uuids.size() - proxiesPendingRestart.size();
                    if (totalProxiesNeeded > i) {
                        //Too many proxies.
                        do {
                            ProxyInfo info = MissionControl.getProxies().get(uuids.remove(0));
                            NetworkManager.removeProxyFromRotation(info);
                            proxiesPendingRestart.add(info);
                            ProtocolMessage message = new ProtocolMessage(Protocol.SHUTDOWN, info.getUuid().toString(), "close", "Mission Control", "");
                            ProxyCommunicationUtils.sendMessage(message);
                            i--;
                        } while (totalProxiesNeeded > i);
                    } else {
                        //Not enough proxies.
                        do {
                            NetworkManager.createProxy(network, false);
                            i++;
                        } while (totalProxiesNeeded < i);
                    }
                }
            } else {
                //Just make sure there are 2 open.
                List<UUID> uuids = MissionControl.getProxies().keySet().stream().filter(uuid -> MissionControl.getProxies().get(uuid).getNetwork() == network).collect(Collectors.toList());
                if (uuids.size() - proxiesPendingRestart.size() > 2) {
                    int i = uuids.size() - proxiesPendingRestart.size();
                    do {
                        ProxyInfo info = MissionControl.getProxies().get(uuids.remove(0));
                        NetworkManager.removeProxyFromRotation(info);
                        NetworkManager.deleteProxy(info);
                        i--;
                    } while (i > 2);
                } else if (uuids.size() - proxiesPendingRestart.size()  < 2) {
                    //While this should technically never be possible, if the network is just starting after all servers were closed, the servers need to be opened.
                    int i = uuids.size() - proxiesPendingRestart.size();
                    do {
                        NetworkManager.createProxy(network, false);
                        i++;
                    } while (i < 2);
                }
            }

            //Now that all proxies are created/deleted, check all gamemodes, making sure that all games have at least 2 servers open.
            for (Game game : Game.values()) {
                if (!game.isEnabled() || !game.isMonitor()) {
                    continue;
                }
                List<ServerInfo> infos = MissionControl.getServers().get(network).values().stream().filter(info -> info.getNetwork() == network && info.getServerType().getString("type").equalsIgnoreCase("game") && info.getServerType().getString("game").equalsIgnoreCase(game.name())).collect(Collectors.toList());
                int serversNeeded = (NetworkManager.getGamePlayerTotals().get(network).get(game) / game.getMaxPlayers()) + ((NetworkManager.getGamePlayerTotals().get(network).get(game) % game.getMaxPlayers() > 0)?1:0) + 1;
                long serversOpen = infos.size() - serversPendingRestart.stream().filter(info -> info.getServerType().getString("game").equalsIgnoreCase(game.name())).count();
                if (NetworkManager.getGamePlayerTotals().get(network).get(game) > 0) {
                    int i = (int) serversOpen;
                    if (serversNeeded < serversOpen) {
                        //Too many servers are open, close as many are needed.
                        do {
                            ServerInfo info = findHighestServerID(infos);
                            infos.remove(info);
                            NetworkManager.removeServerFromRotation(info);
                            serversPendingRestart.add(info);
                            net.auroramc.core.api.backend.communication.ProtocolMessage message = new net.auroramc.core.api.backend.communication.ProtocolMessage(net.auroramc.core.api.backend.communication.Protocol.SHUTDOWN, info.getName(), "close", "Mission Control", "");
                            ServerCommunicationUtils.sendMessage(message, network);
                            i--;
                        } while (serversNeeded < i);
                    } else if (serversNeeded > serversOpen) {
                        //Not enough are open, open as many are needed.
                        do {
                            int id = findLowestAvailableServerID(game, network);
                            NetworkManager.createServer(game.getServerCode() + "-" + id, game, false, network);
                            i++;
                        } while (serversNeeded > i);
                    }
                } else {
                    //No-one is in these servers, just make sure 2 servers are ready to go.
                    int i = (int) serversOpen;
                    if (serversOpen > 2) {
                        //Close as many servers is necessary.
                        do {
                            ServerInfo info = findHighestServerID(infos);
                            infos.remove(info);
                            NetworkManager.removeServerFromRotation(info);
                            NetworkManager.closeServer(info);
                            i--;
                        } while (i > 2);
                    } else if (serversOpen < 2) {
                        //Open as many servers is necessary.
                        do {
                            int id = findLowestAvailableServerID(game, network);
                            NetworkManager.createServer(game.getServerCode() + "-" + id, game, false, network);
                            i++;
                        } while (i < 2);
                    }
                }
            }
        }
    }

    private ServerInfo findHighestServerID(List<ServerInfo> infos) {
        ServerInfo highest = null;
        for (ServerInfo info : infos) {
            if (highest == null) {
                highest = info;
                continue;
            }
            if (Integer.parseInt(highest.getName().split("-")[1]) < Integer.parseInt(info.getName().split("-")[1])) {
                highest = info;
            }
        }
        return highest;
    }

    private int findLowestAvailableServerID(Game game, ServerInfo.Network network) {
        for (int i = 1;i <= 1000;i++) {
            if (!MissionControl.getServers().get(network).containsKey(game.getServerCode() + "-" + i)) {
                return i;
            }
        }
        return -1;
    }

    public static void setUpdate(boolean update) {
        NetworkMonitorRunnable.update = update;
    }

    public static boolean isUpdate() {
        return update;
    }

    public void serverConfirmClose(ServerInfo info) {
        serversPendingRestart.remove(info);
    }

    public void proxyConfirmClose(ProxyInfo info) {
        proxiesPendingRestart.remove(info);
    }
}
