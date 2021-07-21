package net.auroramc.missioncontrol;

import net.auroramc.missioncontrol.backend.Game;
import net.auroramc.missioncontrol.entities.ProxyInfo;
import net.auroramc.missioncontrol.entities.ServerInfo;
import net.auroramc.proxy.api.backend.communication.Protocol;
import net.auroramc.proxy.api.backend.communication.ProtocolMessage;
import net.auroramc.proxy.api.backend.communication.ProxyCommunicationUtils;
import org.apache.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

public class NetworkMonitorRunnable implements Runnable {

    private static boolean update;
    private final Logger logger;
    private final ServerInfo.Network network;

    public NetworkMonitorRunnable(Logger logger, ServerInfo.Network network) {
        this.logger = logger;
        update = false;
        this. network = network;
    }

    @Override
    public void run() {
        //If an update is not in progress, check player counts.
        if (!update) {
            //Check to see if there are sufficient/too many connection nodes open. Open/close as many as are needed. Keep open as many connection nodes are needed to support all the players + 1. (or +2 if there is no-one online)
            if (NetworkManager.getNetworkPlayerTotal().get(network) > 0) {
                List<UUID> uuids = MissionControl.getProxies().keySet().stream().filter(uuid -> MissionControl.getProxies().get(uuid).getNetwork() == network).collect(Collectors.toList());
                int totalProxiesNeeded = (NetworkManager.getNetworkPlayerTotal().get(network) / 200) + ((NetworkManager.getNetworkPlayerTotal().get(network) % 200 > 0)?1:0);
                if (uuids.size() != totalProxiesNeeded) {
                    //There are not enough/too many proxies open. Close/open some.
                    if (totalProxiesNeeded > uuids.size()) {
                        //Too many proxies.
                        do {
                            ProxyInfo info = MissionControl.getProxies().get(uuids.remove(0));
                            NetworkManager.removeProxyFromRotation(info);
                            ProtocolMessage message = new ProtocolMessage(Protocol.SHUTDOWN, info.getUuid().toString(), "close", "Mission Control", "");
                            ProxyCommunicationUtils.sendMessage(message);
                        } while (totalProxiesNeeded > uuids.size());
                    } else {
                        //Not enough proxies.
                        int i = uuids.size();
                        do {
                            NetworkManager.createProxy(network, false);
                            i++;
                        } while (totalProxiesNeeded < i);
                    }
                }
            } else {
                //Just make sure there are 2 open.
                List<UUID> uuids = MissionControl.getProxies().keySet().stream().filter(uuid -> MissionControl.getProxies().get(uuid).getNetwork() == network).collect(Collectors.toList());
                if (uuids.size() > 2) {
                    do {
                        ProxyInfo info = MissionControl.getProxies().get(uuids.remove(0));
                        NetworkManager.removeProxyFromRotation(info);
                        NetworkManager.deleteProxy(info);
                    } while (uuids.size() > 2);
                } else if (MissionControl.getProxies().size() < 2) {
                    //While this should technically never be possible, if the network is just starting after all servers were closed, the servers need to be opened.
                    int i = uuids.size();
                    do {
                        NetworkManager.createProxy(network, false);
                        i++;
                    } while (i < 2);
                }
            }

            //Now that all proxies are created/deleted, check all gamemodes, making sure that all games have at least 2 servers open.
            for (Game game : Game.values()) {

            }
        }
    }

    public static void setUpdate(boolean update) {
        NetworkMonitorRunnable.update = update;
    }
}
