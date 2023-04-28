/*
 * Copyright (c) 2021 AuroraMC Ltd. All Rights Reserved.
 */

package net.auroramc.missioncontrol;

import net.auroramc.proxy.api.backend.communication.Protocol;
import net.auroramc.proxy.api.backend.communication.ProtocolMessage;
import net.auroramc.core.api.backend.communication.ServerCommunicationUtils;
import net.auroramc.missioncontrol.backend.util.Module;
import net.auroramc.missioncontrol.entities.ProxyInfo;
import net.auroramc.missioncontrol.entities.RestartServerResponse;
import net.auroramc.missioncontrol.entities.ServerInfo;
import net.auroramc.proxy.api.backend.communication.ProxyCommunicationUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ProxyRestarterThread extends Thread {

    private final ServerInfo.Network network;

    private final List<ProxyInfo> serversToRestart = new ArrayList<>();
    private final List<ProxyInfo> serversPendingRestart = new ArrayList<>();
    private final Logger logger;

    private final ArrayBlockingQueue<RestartServerResponse> queue = new ArrayBlockingQueue<>(50);

    public ProxyRestarterThread(ServerInfo.Network network) {
        this.network = network;
        logger = MissionControl.getLogger();
    }

    @Override
    public void run() {
        logger.info("Initiating proxy restart for network '" + network.name() + "'.");
        addServers(MissionControl.getProxies().values().stream().filter(info -> info.getNetwork() == network).collect(Collectors.toList()));
        if (serversToRestart.size() > 10) {
            for (int i = 0;i < 5;i++) {
                queueForRestart(serversToRestart.remove(0));
            }
        } else {
            queueForRestart(serversToRestart.remove(0));
        }
        while (true) {
            RestartServerResponse response;
            try {
                response = queue.poll(10, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                e.printStackTrace();
                break;
            }
            if (response != null) {
                if (response.getProtocol() == RestartServerResponse.Type.CONFIRM_CLOSE) {
                    ProxyInfo info = (ProxyInfo) response.getInfo();
                    info.setStatus(ProxyInfo.ProxyStatus.RESTARTING);
                    if (network == ServerInfo.Network.ALPHA) {
                        info.setBuildNumber(NetworkManager.getAlphaBuilds().get(Module.CORE));
                        info.setBranch(NetworkManager.getAlphaBranches().get(Module.CORE));
                    } else if (network == ServerInfo.Network.MAIN) {
                        info.setBuildNumber(NetworkManager.getCurrentCoreBuildNumber());
                    }
                    MissionControl.getPanelManager().updateProxy(info);
                } else {
                    ProxyInfo info = (ProxyInfo) response.getInfo();
                    serversPendingRestart.remove(info);
                    info.setPlayerCount((byte)0);
                    info.setStatus(ProxyInfo.ProxyStatus.ONLINE);
                    if (serversToRestart.size() > 0) {
                        info = serversToRestart.remove(0);
                        queueForRestart(info);
                    } else {
                        if (serversPendingRestart.size() == 0) {
                            NetworkManager.proxyUpdateComplete();
                            return;
                        }
                    }
                }
            } else {
                boolean more = false;
                for (ProxyInfo info : serversToRestart) {
                    if (network == ServerInfo.Network.ALPHA && info.getBuildNumber() != NetworkManager.getAlphaBuilds().get(Module.CORE)) {
                        more = true;
                        break;
                    } else if (info.getBuildNumber() != NetworkManager.getCurrentCoreBuildNumber()) {
                        more = true;
                        break;
                    }
                }
                if (!more) {
                    if (serversPendingRestart.size() > 0) {
                        boolean waitingRestart = false;
                        for (ProxyInfo info : serversPendingRestart) {
                            if (info.getStatus() == ProxyInfo.ProxyStatus.PENDING_RESTART) {
                                waitingRestart = true;
                            }
                        }
                        if (!waitingRestart) {
                            break;
                        }
                    } else {
                        break;
                    }

                }
            }

        }

        NetworkManager.proxyUpdateComplete();
    }

    public void queueForRestart(ProxyInfo info) {
        info.setStatus(ProxyInfo.ProxyStatus.PENDING_RESTART);
        serversPendingRestart.add(info);
        ProtocolMessage message = new ProtocolMessage(Protocol.SHUTDOWN, info.getUuid().toString(), "update", "Mission Control", "");
        ProxyCommunicationUtils.sendMessage(message);
    }

    /**
     * Util method for preventing duplicate restarts.
     * @param infos list of servers to add to the restart queue.
     */
    private void addServers(List<ProxyInfo> infos) {
        outer:
        for (ProxyInfo info : infos) {
            if (info == null) {
                continue;
            }
            for (ProxyInfo info2 : serversToRestart) {
                if (info.getUuid().equals(info2.getUuid())) {
                    continue outer;
                }
            }
            serversToRestart.add(info);
        }
    }

    public synchronized void proxyCloseConfirm(ProxyInfo info) {
        queue.add(new RestartServerResponse(info, RestartServerResponse.Type.CONFIRM_CLOSE));
    }
    public synchronized void proxyStartConfirm(ProxyInfo info) {
        queue.add(new RestartServerResponse(info, RestartServerResponse.Type.CONFIRM_OPEN));
    }

    public ServerInfo.Network getNetwork() {
        return network;
    }

    public enum RestartMode {BATCHES, SOLO}
}
