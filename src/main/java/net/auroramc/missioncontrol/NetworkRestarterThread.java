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
    private final List<ServerInfo> lobbiesToRestart = new ArrayList<>();
    private final List<ProxyInfo> proxiesToRestart = new ArrayList<>();
    private RestartMode proxyRestartMode;
    private int totalUpdates;
    private Logger logger;

    private final ArrayBlockingQueue<RestartServerResponse> queue = new ArrayBlockingQueue<>(50);

    public NetworkRestarterThread(List<Module> modulesToRestart, ServerInfo.Network network) {
        modules = modulesToRestart;
        this.network = network;
        logger = MissionControl.getLogger();
    }

    @Override
    public void run() {
        logger.info("Initiating restart for network '" + network.name() + "', restarting " + modules.size() + " modules.");
        if (modules.contains(Module.PROXY)) {
            //Open and close proxies in batches of 10 to prevent using too many resources.
            proxiesToRestart.addAll(MissionControl.getProxies().values().stream().filter(info -> info.getNetwork() == network).collect(Collectors.toList()));
            totalUpdates = proxiesToRestart.size();
            for (ProxyInfo info : proxiesToRestart) {
                info.setBuildNumber(NetworkManager.getCurrentProxyBuildNumber());
            }
            if (proxiesToRestart.size() <= 20) {
                //As there are only 20 open, just restart them 1 at a time.
                proxyRestartMode = RestartMode.SOLO;
                ProxyInfo info = proxiesToRestart.remove(0);
                if (info != null) {
                    NetworkManager.removeProxyFromRotation(info);
                    net.auroramc.proxy.api.backend.communication.ProtocolMessage message = new net.auroramc.proxy.api.backend.communication.ProtocolMessage(net.auroramc.proxy.api.backend.communication.Protocol.SHUTDOWN, info.getUuid().toString(), "update", "Mission Control", "");
                    ProxyCommunicationUtils.sendMessage(message);
                }
            } else {
                //Create 10 connection nodes, for each connection node, remove 1 from rotation and initiate shutdown on it.
                for (int i = 0;i < 10;i++) {
                    ProxyInfo info = proxiesToRestart.remove(0);
                    NetworkManager.createProxy(network, info.isForced(), true);
                    NetworkManager.removeProxyFromRotation(info);
                    net.auroramc.proxy.api.backend.communication.ProtocolMessage message = new net.auroramc.proxy.api.backend.communication.ProtocolMessage(net.auroramc.proxy.api.backend.communication.Protocol.SHUTDOWN, info.getUuid().toString(), "update", "Mission Control", "");
                    ProxyCommunicationUtils.sendMessage(message);
                }
            }
        }
        if (modules.contains(Module.CORE)) {
            //Restart the entire network.
            for (ServerInfo info : MissionControl.getServers().get(network).values().stream().filter(inf -> inf.getNetwork() == network).collect(Collectors.toList())) {
                info.setBuildNumber(NetworkManager.getCurrentCoreBuildNumber());
                if (info.getServerType().getString("type").equalsIgnoreCase("lobby")) {
                    lobbiesToRestart.add(info);
                } else {
                    serversToRestart.add(info);
                }
            }

            updateLobbies();
            updateServers();
        } else {
            if (modules.contains(Module.BUILD)) {
                //Restart any build servers.
                List<ServerInfo> servers = MissionControl.getServers().get(network).values().stream().filter(server -> server.getServerType().getString("type").equalsIgnoreCase("build") && server.getNetwork() == network).collect(Collectors.toList());
                for (ServerInfo info : servers) {
                    info.setBuildBuildNumber(NetworkManager.getCurrentBuildBuildNumber());
                    ProtocolMessage message = new ProtocolMessage(Protocol.SHUTDOWN, info.getName(), "update", "Mission Control", "");
                    ServerCommunicationUtils.sendMessage(message, network);
                }
            }
            if (modules.contains(Module.ENGINE) || modules.contains(Module.GAME)) {
                //Restart any game servers.
                serversToRestart.addAll(MissionControl.getServers().get(network).values().stream().filter(info -> !info.getServerType().getString("type").equalsIgnoreCase("lobby") && info.getNetwork() == network).collect(Collectors.toList()));
                for (ServerInfo info : serversToRestart) {
                    info.setGameBuildNumber(NetworkManager.getCurrentGameBuildNumber());
                    info.setEngineBuildNumber(NetworkManager.getCurrentEngineBuildNumber());
                }
                updateServers();
            }
            if (modules.contains(Module.LOBBY)) {
                //Restart any lobby servers.
                lobbiesToRestart.addAll(MissionControl.getServers().get(network).values().stream().filter(info -> info.getServerType().getString("type").equalsIgnoreCase("lobby") && info.getNetwork() == network).collect(Collectors.toList()));
                for (ServerInfo info : serversToRestart) {
                    info.setLobbyBuildNumber(NetworkManager.getCurrentLobbyBuildNumber());
                }
                updateLobbies();
            }
            if (modules.contains(Module.EVENT)) {
                //Restart any event servers currently active. Does not include servers that have been turned into event servers.
                List<ServerInfo> servers = MissionControl.getServers().get(network).values().stream().filter(server -> server.getServerType().getString("type").equalsIgnoreCase("game") && server.getServerType().getBoolean("event") && server.getNetwork() == network).collect(Collectors.toList());
                for (ServerInfo info : servers) {
                    ProtocolMessage message = new ProtocolMessage(Protocol.SHUTDOWN, info.getName(), "update", "Mission Control", "");
                    ServerCommunicationUtils.sendMessage(message, network);
                }
            }
        }

        //Update has been started. Now listen for changes then restart more servers.
        while (true) {
            RestartServerResponse response;
            try {
                response = queue.poll(10, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                e.printStackTrace();
                NetworkManager.updateComplete();
                return;
            }
            if (response != null) {
                if (response.getProtocol() == RestartServerResponse.Type.CONFIRM_CLOSE) {
                    if (response.getInfo() instanceof ProxyInfo) {
                        MissionControl.getPanelManager().closeServer(((ProxyInfo) response.getInfo()).getUuid().toString(), network);
                        if (proxyRestartMode == RestartMode.BATCHES) {
                            //Previous connection node has been closed, open a new connection node then prompt another connection node to close.
                            NetworkManager.deleteProxy(((ProxyInfo) response.getInfo()));
                            NetworkManager.createProxy(network, ((ProxyInfo) response.getInfo()).isForced(), true);
                            ProxyInfo info = proxiesToRestart.remove(0);
                            NetworkManager.removeProxyFromRotation(info);
                            net.auroramc.proxy.api.backend.communication.ProtocolMessage message = new net.auroramc.proxy.api.backend.communication.ProtocolMessage(net.auroramc.proxy.api.backend.communication.Protocol.SHUTDOWN, info.getUuid().toString(), "update", "Mission Control", "");
                            ProxyCommunicationUtils.sendMessage(message);
                        } else {
                            //A connection node has closed, update then re-open the connection node.
                            MissionControl.getPanelManager().closeServer(((ProxyInfo) response.getInfo()).getUuid().toString(), network);
                            MissionControl.getPanelManager().updateProxy((ProxyInfo) response.getInfo());
                            MissionControl.getPanelManager().openServer(((ProxyInfo) response.getInfo()).getUuid().toString(), network);
                        }
                    } else {
                        ServerInfo info = (ServerInfo) response.getInfo();
                        //Close the server, update it, then restart it.
                        MissionControl.getPanelManager().closeServer(info.getName(), network);
                        MissionControl.getPanelManager().updateServer(info);
                        MissionControl.getPanelManager().openServer(info.getName(), network);
                    }
                } else {
                    totalUpdates--;
                    if (response.getInfo() instanceof ProxyInfo) {
                        if (proxyRestartMode == RestartMode.SOLO) {
                            //The connection node has started and is ready to accept connections, add it to the rotation and then queue another node to restart.
                            MissionControl.getProxyManager().addServer((ProxyInfo) response.getInfo());

                            if (totalUpdates == 0) {
                                NetworkManager.updateComplete();
                                return;
                            }
                            ProxyInfo info = proxiesToRestart.remove(0);
                            NetworkManager.removeProxyFromRotation(info);
                            net.auroramc.proxy.api.backend.communication.ProtocolMessage message = new net.auroramc.proxy.api.backend.communication.ProtocolMessage(net.auroramc.proxy.api.backend.communication.Protocol.SHUTDOWN, info.getUuid().toString(), "update", "Mission Control", "");
                            ProxyCommunicationUtils.sendMessage(message);
                        }
                    } else {
                        ((ServerInfo) response.getInfo()).setPlayerCount((byte) 0);
                        if (totalUpdates == 0) {
                            NetworkManager.updateComplete();
                            return;
                        }
                        ServerInfo info = null;
                        if (((ServerInfo) response.getInfo()).getServerType().getString("type").equalsIgnoreCase("lobby")) {
                            if (lobbiesToRestart.size() > 0) {
                                info = lobbiesToRestart.remove(0);
                            }
                        } else {
                            if (serversToRestart.size() > 0) {
                                info = serversToRestart.remove(0);
                            }
                        }
                        if (info != null) {
                            ProtocolMessage message = new ProtocolMessage(Protocol.SHUTDOWN, info.getName(), "update", "Mission Control", "");
                            ServerCommunicationUtils.sendMessage(message, network);
                        }
                    }
                }
            }

            if (totalUpdates == 0) {
                NetworkManager.updateComplete();
                return;
            }
        }
    }

    private void updateLobbies() {
        update(lobbiesToRestart);
    }

    private void updateServers() {
        update(serversToRestart);
    }

    private void update(List<ServerInfo> serversToRestart) {
        totalUpdates += serversToRestart.size();
        if (serversToRestart.size() <= 20) {
            if (serversToRestart.size() > 0) {
                ServerInfo info = serversToRestart.remove(0);
                ProtocolMessage message = new ProtocolMessage(Protocol.SHUTDOWN, info.getName(), "update", "Mission Control", "");
                ServerCommunicationUtils.sendMessage(message, network);
            }
        } else {
            for (int i = 0;i < 10;i++) {
                ServerInfo info = serversToRestart.remove(0);
                ProtocolMessage message = new ProtocolMessage(Protocol.SHUTDOWN, info.getName(), "update", "Mission Control", "");
                ServerCommunicationUtils.sendMessage(message, network);
            }
        }
    }

    public synchronized void proxyCloseConfirm(ProxyInfo info) {
        queue.add(new RestartServerResponse(info, RestartServerResponse.Type.CONFIRM_CLOSE));
    }

    public synchronized void proxyStartConfirm(ProxyInfo info) {
        queue.add(new RestartServerResponse(info, RestartServerResponse.Type.CONFIRM_OPEN));
    }

    public synchronized void serverCloseConfirm(ServerInfo info) {
        queue.add(new RestartServerResponse(info, RestartServerResponse.Type.CONFIRM_CLOSE));
    }
    public synchronized void serverStartConfirm(ServerInfo info) {
        queue.add(new RestartServerResponse(info, RestartServerResponse.Type.CONFIRM_OPEN));
    }

    public RestartMode getProxyRestartMode() {
        return proxyRestartMode;
    }

    public ServerInfo.Network getNetwork() {
        return network;
    }

    public enum RestartMode {BATCHES, SOLO}
}
