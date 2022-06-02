/*
 * Copyright (c) 2021 AuroraMC Ltd. All Rights Reserved.
 */

package net.auroramc.missioncontrol;

import net.auroramc.core.api.backend.communication.Protocol;
import net.auroramc.core.api.backend.communication.ProtocolMessage;
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

public class NetworkRestarterThread extends Thread {

    private final ServerInfo.Network network;

    private final List<Module> modules;
    private final List<ServerInfo> serversToRestart = new ArrayList<>();
    private final List<ServerInfo> serversPendingRestart = new ArrayList<>();
    private final Logger logger;

    private final ArrayBlockingQueue<RestartServerResponse> queue = new ArrayBlockingQueue<>(50);

    public NetworkRestarterThread(List<Module> modulesToRestart, ServerInfo.Network network) {
        modules = modulesToRestart;
        this.network = network;
        logger = MissionControl.getLogger();
    }

    @Override
    public void run() {
        logger.info("Initiating restart for network '" + network.name() + "', restarting " + modules.size() + " modules.");
        if (modules.contains(Module.CORE)) {
            serversToRestart.addAll(MissionControl.getServers().get(network).values());
        } else {
            if (modules.contains(Module.GAME) || modules.contains(Module.ENGINE)) {
                addServers(MissionControl.getServers().get(network).values().stream().filter(info -> info.getServerType().getString("type").equals("game") || info.getServerType().getString("type").equals("staff")).collect(Collectors.toList()));
            } else if (modules.contains(Module.EVENT)) {
                addServers(MissionControl.getServers().get(network).values().stream().filter(info -> info.getServerType().getString("type").equals("game") && info.getServerType().has("event")).collect(Collectors.toList()));
            }
            if (modules.contains(Module.LOBBY)) {
                addServers(MissionControl.getServers().get(network).values().stream().filter(info -> info.getServerType().getString("type").equals("lobby")).collect(Collectors.toList()));
            }
            if (modules.contains(Module.BUILD)) {
                addServers(MissionControl.getServers().get(network).values().stream().filter(info -> info.getServerType().getString("type").equals("build")).collect(Collectors.toList()));
            }
            if (modules.contains(Module.DUELS)) {
                addServers(MissionControl.getServers().get(network).values().stream().filter(info -> info.getServerType().getString("type").equals("duels")).collect(Collectors.toList()));
            }
        }
        if (serversToRestart.size() > 10) {
            for (int i = 0;i < 5;i++) {
                queueForRestart(serversToRestart.remove(0));
            }
        } else {
            queueForRestart(serversToRestart.remove(0));
        }

        outer:
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
                    ServerInfo info = (ServerInfo) response.getInfo();
                    info.setStatus(ServerInfo.ServerStatus.RESTARTING);
                    if (network != ServerInfo.Network.ALPHA) {
                        if (info.getBuildBuildNumber() > 0) {
                            info.setBuildBuildNumber(NetworkManager.getCurrentBuildBuildNumber());
                        }
                        if (info.getEngineBuildNumber() > 0) {
                            info.setEngineBuildNumber(NetworkManager.getCurrentEngineBuildNumber());
                        }
                        if (info.getGameBuildNumber() > 0) {
                            info.setGameBuildNumber(NetworkManager.getCurrentGameBuildNumber());
                        }
                        if (info.getLobbyBuildNumber() > 0) {
                            info.setLobbyBuildNumber(NetworkManager.getCurrentLobbyBuildNumber());
                        }
                        if (info.getDuelsBuildNumber() > 0) {
                            info.setDuelsBuildNumber(NetworkManager.getCurrentDuelsBuildNumber());
                        }
                        info.setBuildNumber(NetworkManager.getCurrentCoreBuildNumber());
                    } else {
                        if (info.getBuildBuildNumber() > 0) {
                            info.setBuildBuildNumber(NetworkManager.getAlphaBuilds().get(Module.BUILD));
                        }
                        if (info.getEngineBuildNumber() > 0) {
                            info.setEngineBuildNumber(NetworkManager.getAlphaBuilds().get(Module.ENGINE));
                        }
                        if (info.getGameBuildNumber() > 0) {
                            info.setGameBuildNumber(NetworkManager.getAlphaBuilds().get(Module.GAME));
                        }
                        if (info.getLobbyBuildNumber() > 0) {
                            info.setLobbyBuildNumber(NetworkManager.getAlphaBuilds().get(Module.LOBBY));
                        }
                        if (info.getDuelsBuildNumber() > 0) {
                            info.setDuelsBuildNumber(NetworkManager.getAlphaBuilds().get(Module.DUELS));
                        }
                        info.setBuildNumber(NetworkManager.getAlphaBuilds().get(Module.CORE));
                    }
                    MissionControl.getPanelManager().updateServer(info);
                } else {
                    ServerInfo info = (ServerInfo) response.getInfo();
                    serversPendingRestart.remove(info);
                    info.setPlayerCount((byte)0);
                    info.setStatus(ServerInfo.ServerStatus.ONLINE);
                    while (serversToRestart.size() > 0) {
                        info = serversToRestart.remove(0);
                        boolean restart = false;
                        if (info.getBuildBuildNumber() > 0) {
                            restart = info.getBuildBuildNumber() != NetworkManager.getCurrentBuildBuildNumber();
                        }
                        if (info.getEngineBuildNumber() > 0) {
                            restart = restart || info.getBuildBuildNumber() != NetworkManager.getCurrentEngineBuildNumber();
                        }
                        if (info.getGameBuildNumber() > 0) {
                            restart = restart || info.getBuildBuildNumber() != NetworkManager.getCurrentGameBuildNumber();
                        }
                        if (info.getLobbyBuildNumber() > 0) {
                            restart = restart || info.getBuildBuildNumber() != NetworkManager.getCurrentLobbyBuildNumber();
                        }
                        if (info.getDuelsBuildNumber() > 0) {
                            restart = restart || info.getDuelsBuildNumber() != NetworkManager.getCurrentDuelsBuildNumber();
                        }
                        restart = restart || info.getBuildBuildNumber() != NetworkManager.getCurrentCoreBuildNumber();
                        if (restart) {
                            queueForRestart(info);
                            continue outer;
                        }
                    }
                    if (serversPendingRestart.size() == 0) {
                        NetworkManager.updateComplete();
                        return;
                    }
                }
            } else {
                boolean more = false;
                if (network != ServerInfo.Network.ALPHA) {
                    for (ServerInfo info : serversToRestart) {
                        if (modules.contains(Module.CORE)) {
                            if (info.getBuildNumber() != NetworkManager.getCurrentCoreBuildNumber()) {
                                more  = true;
                                break;
                            }
                        }
                        if (modules.contains(Module.LOBBY)) {
                            if (info.getLobbyBuildNumber() != NetworkManager.getCurrentLobbyBuildNumber()) {
                                more  = true;
                                break;
                            }
                        }
                        if (modules.contains(Module.GAME) || modules.contains(Module.ENGINE)) {
                            if (info.getEngineBuildNumber() != NetworkManager.getCurrentEngineBuildNumber() || info.getGameBuildNumber() != NetworkManager.getCurrentGameBuildNumber()) {
                                more  = true;
                                break;
                            }
                        }
                        if (modules.contains(Module.BUILD)) {
                            if (info.getBuildBuildNumber() != NetworkManager.getCurrentBuildBuildNumber()) {
                                more  = true;
                                break;
                            }
                        }
                        if (modules.contains(Module.DUELS)) {
                            if (info.getDuelsBuildNumber() != NetworkManager.getCurrentDuelsBuildNumber()) {
                                more  = true;
                                break;
                            }
                        }
                    }
                } else {
                    for (ServerInfo info : serversToRestart) {
                        if (modules.contains(Module.CORE)) {
                            if (info.getBuildNumber() != NetworkManager.getAlphaBuilds().get(Module.CORE)) {
                                more  = true;
                                break;
                            }
                        }
                        if (modules.contains(Module.LOBBY)) {
                            if (info.getLobbyBuildNumber() != NetworkManager.getAlphaBuilds().get(Module.LOBBY)) {
                                more  = true;
                                break;
                            }
                        }
                        if (modules.contains(Module.GAME) || modules.contains(Module.ENGINE)) {
                            if (info.getEngineBuildNumber() != NetworkManager.getAlphaBuilds().get(Module.ENGINE) || info.getGameBuildNumber() != NetworkManager.getAlphaBuilds().get(Module.GAME)) {
                                more  = true;
                                break;
                            }
                        }
                        if (modules.contains(Module.BUILD)) {
                            if (info.getBuildBuildNumber() != NetworkManager.getAlphaBuilds().get(Module.BUILD)) {
                                more  = true;
                                break;
                            }
                        }
                        if (modules.contains(Module.DUELS)) {
                            if (info.getDuelsBuildNumber() != NetworkManager.getAlphaBuilds().get(Module.DUELS)) {
                                more  = true;
                                break;
                            }
                        }
                    }
                }
                if (!more) {
                    if (serversPendingRestart.size() > 0) {
                        boolean waitingRestart = false;
                        for (ServerInfo info : serversPendingRestart) {
                            if (info.getStatus() == ServerInfo.ServerStatus.PENDING_RESTART) {
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

        NetworkManager.updateComplete();
    }

    public void queueForRestart(ServerInfo info) {
        info.setStatus(ServerInfo.ServerStatus.PENDING_RESTART);
        serversPendingRestart.add(info);
        ProtocolMessage message = new ProtocolMessage(Protocol.SHUTDOWN, info.getName(), "update", "Mission Control", "");
        ServerCommunicationUtils.sendMessage(message, network);
    }

    /**
     * Util method for preventing duplicate restarts.
     * @param infos list of servers to add to the restart queue.
     */
    private void addServers(List<ServerInfo> infos) {
        outer:
        for (ServerInfo info : infos) {
            if (info == null) {
                continue;
            }
            for (ServerInfo info2 : serversToRestart) {
                if (info.getName().equals(info2.getName())) {
                    continue outer;
                }
            }
            serversToRestart.add(info);
        }
    }

    public synchronized void serverCloseConfirm(ServerInfo info) {
        queue.add(new RestartServerResponse(info, RestartServerResponse.Type.CONFIRM_CLOSE));
    }
    public synchronized void serverStartConfirm(ServerInfo info) {
        queue.add(new RestartServerResponse(info, RestartServerResponse.Type.CONFIRM_OPEN));
    }

    public ServerInfo.Network getNetwork() {
        return network;
    }

    public enum RestartMode {BATCHES, SOLO}
}
