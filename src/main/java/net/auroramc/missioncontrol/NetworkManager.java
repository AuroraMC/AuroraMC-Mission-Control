/*
 * Copyright (c) 2021 AuroraMC Ltd. All Rights Reserved.
 */

package net.auroramc.missioncontrol;

import com.mattmalec.pterodactyl4j.application.entities.Allocation;
import com.mattmalec.pterodactyl4j.application.entities.Node;
import net.auroramc.core.api.backend.communication.Protocol;
import net.auroramc.core.api.backend.communication.ProtocolMessage;
import net.auroramc.core.api.backend.communication.ServerCommunicationUtils;
import net.auroramc.missioncontrol.backend.*;
import net.auroramc.missioncontrol.backend.managers.CommandManager;
import net.auroramc.missioncontrol.backend.managers.DatabaseManager;
import net.auroramc.missioncontrol.entities.ProxyInfo;
import net.auroramc.missioncontrol.entities.ServerInfo;
import net.auroramc.proxy.api.backend.communication.ProxyCommunicationUtils;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static net.auroramc.missioncontrol.entities.ServerInfo.Network.*;

public class NetworkManager {

    private static final Object lock = new Object();
    private static final Object lock2 = new Object();
    private static final Object lock3 = new Object();
    private static boolean shutdown;

    private static final ArrayBlockingQueue<ProxyInfo> proxyBlockingQueue = new ArrayBlockingQueue<>(10);
    private static final ArrayBlockingQueue<ServerInfo> serverBlockingQueue = new ArrayBlockingQueue<>(20);

    /*
     * Build numbers for each module.
     */
    private static int currentCoreBuildNumber;
    private static int currentLobbyBuildNumber;
    private static int currentBuildBuildNumber;
    private static int currentProxyBuildNumber;
    private static int currentEngineBuildNumber;
    private static int currentGameBuildNumber;

    private static final Map<ServerInfo.Network, Integer> networkPlayerTotal;
    private static final Map<ServerInfo.Network, Map<Game, Integer>> gamePlayerTotals;
    private static final Map<ServerInfo.Network, Map<String, Integer>> serverPlayerTotals;
    private static final Map<ServerInfo.Network, Map<UUID, Integer>> nodePlayerTotals;
    private static final Logger logger;
    private static final DatabaseManager dbManager;
    private static final ScheduledExecutorService scheduler;

    private static Map<ServerInfo.Network, String> motd;
    private static Map<ServerInfo.Network, Boolean> maintenance;
    private static Map<ServerInfo.Network, MaintenanceMode> maintenanceMode;
    private static Map<ServerInfo.Network, String> maintenanceMotd;

    private static List<Node> nodes;
    private static NetworkRestarterThread restarterThread;
    private static NetworkMonitorRunnable monitorRunnable;
    private static NetworkMonitorRunnable alphaMonitorRunnable;

    private static Map<ServerInfo.Network, Map<Game, Boolean>> gameEnabled;
    private static Map<ServerInfo.Network, Map<Game, Boolean>> gameMonitor;

    private static Map<Module, String> alphaBranches;
    private static Map<Module, Integer> alphaBuilds;

    private static Map<ServerInfo.Network, Boolean> serverMonitorEnabled;
    private static boolean alphaEnabled;


    static {

        logger = MissionControl.getLogger();
        dbManager = MissionControl.getDbManager();

        networkPlayerTotal = new HashMap<>();
        gamePlayerTotals = new HashMap<>();
        serverPlayerTotals = new HashMap<>();
        nodePlayerTotals = new HashMap<>();
        serverMonitorEnabled = new HashMap<>();

        for (ServerInfo.Network network : ServerInfo.Network.values()) {
            networkPlayerTotal.put(network, 0);

            Map<Game, Integer> gameTotals = new HashMap<>();
            for (Game game : Game.values()) {
                gameTotals.put(game, 0);
            }

            gamePlayerTotals.put(network, gameTotals);
            serverPlayerTotals.put(network, new HashMap<>());
            nodePlayerTotals.put(network, new HashMap<>());
            if (network == TEST) continue;
            serverMonitorEnabled.put(network, dbManager.isServerManagerEnabled(network));

        }

        nodes = MissionControl.getPanelManager().getAllNodes();

        scheduler = Executors.newScheduledThreadPool(4);
        shutdown = false;
    }


    public static void handoff() {
        logger.info("Fetching pushed builds...");
        currentCoreBuildNumber = dbManager.getCurrentCoreBuildNumber();
        currentBuildBuildNumber = dbManager.getCurrentBuildBuildNumber();
        currentEngineBuildNumber = dbManager.getCurrentEngineBuildNumber();
        currentGameBuildNumber = dbManager.getCurrentGameBuildNumber();
        currentLobbyBuildNumber = dbManager.getCurrentLobbyBuildNumber();
        currentProxyBuildNumber = dbManager.getCurrentProxyBuildNumber();

        motd = dbManager.getMotd();
        maintenance = dbManager.getMaintenance();
        maintenanceMode = dbManager.getMaintenanceMode();
        maintenanceMotd = dbManager.getMaintenanceMotd();

        alphaBranches = dbManager.getBranchMappings();
        alphaBuilds = dbManager.getBuildMappings();

        gameEnabled = new HashMap<>();
        for (ServerInfo.Network network : ServerInfo.Network .values()) {
            gameEnabled.put(network, dbManager.getGameEnabled(network));
        }

        gameMonitor = new HashMap<>();
        for (ServerInfo.Network network : ServerInfo.Network .values()) {
            gameMonitor.put(network, dbManager.getMonitoring(network));
        }

        logger.info("Requesting player counts from servers...");
        for (ServerInfo.Network network : ServerInfo.Network.values()) {
            for (ServerInfo info : MissionControl.getServers().get(network).values()) {
                ProtocolMessage message = new ProtocolMessage(Protocol.UPDATE_PLAYER_COUNT, info.getName(), "update", "MissionControl", "");
                ServerCommunicationUtils.sendMessage(message, network);
            }
        }
        for (ProxyInfo info : MissionControl.getProxies().values()) {
            net.auroramc.proxy.api.backend.communication.ProtocolMessage message = new net.auroramc.proxy.api.backend.communication.ProtocolMessage(net.auroramc.proxy.api.backend.communication.Protocol.UPDATE_PLAYER_COUNT, info.getUuid().toString(), "update", "MissionControl", "");
            ProxyCommunicationUtils.sendMessage(message);
        }
        logger.info("Requests sent. Awaiting responses...");

        //Wait for a response from all of the servers (max 1 minute wait)
        try {
            synchronized (lock2) {
                for (int i = 0;i <= 6;i++) {
                    int totalServerTotals = serverPlayerTotals.get(MAIN).size() + serverPlayerTotals.get(TEST).size() + serverPlayerTotals.get(ALPHA).size();
                    int totalProxyTotals = nodePlayerTotals.get(MAIN).size() + nodePlayerTotals.get(TEST).size() + nodePlayerTotals.get(ALPHA).size();

                    int totalServers = MissionControl.getServers().get(MAIN).size() + MissionControl.getServers().get(TEST).size() + MissionControl.getServers().get(ALPHA).size();
                    if (totalServerTotals == totalServers && totalProxyTotals == MissionControl.getProxies().size()) {
                        logger.info("All responses received, starting network monitoring thread...");
                        break;
                    }
                    if (i == 6) {
                        logger.info("Not all responses received but timeout reached, starting network monitoring thread...");
                    } else {
                        logger.info("Still waiting for " + ((totalServers - totalServerTotals) + (MissionControl.getProxies().size() - totalProxyTotals)) + " responses...");
                        lock2.wait(10000);
                    }
                }
            }

        } catch (InterruptedException e) {
            logger.log(Level.WARNING,"Waiting for responses was interrupted. Starting network monitoring thread... ", e);
        }


        if (serverMonitorEnabled.get(MAIN)) {
            monitorRunnable = new NetworkMonitorRunnable(logger, MAIN);
            scheduler.scheduleWithFixedDelay(monitorRunnable, 1, 1, TimeUnit.MINUTES);

        }

        alphaEnabled = MissionControl.getDbManager().isAlphaEnabled();
        if (alphaEnabled && serverMonitorEnabled.get(ALPHA)) {
            alphaMonitorRunnable = new NetworkMonitorRunnable(logger, ALPHA);
            scheduler.scheduleWithFixedDelay(alphaMonitorRunnable, 1, 1, TimeUnit.MINUTES);
        }

        scheduler.scheduleWithFixedDelay(new PlayerCountUpdateRunnable(), 1, 1, TimeUnit.SECONDS);
        scheduler.scheduleWithFixedDelay(new RequestPlayerCountUpdateRunnable(), 5, 5, TimeUnit.MINUTES);

        done();
    }

    private static void done() {
        logger.info("Handoff complete.");
        String command;
        try {
            while (!shutdown && ( command = MissionControl.getConsoleReader().readLine( ">" ) ) != null ) {
                CommandManager.onCommand(command);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "An error has occurred while trying to process commands. Shutting down. Stack trace: ", e);
        }
        logger.info("Shutting down...");
        shutdown();
    }

    public static void interrupt() {
        shutdown = true;
    }

    public static void pushUpdate(Map<Module, Integer> modules, ServerInfo.Network network) {
        //Stop the network monitor from spinning up more servers.
        if (network == ServerInfo.Network.ALPHA) {
            if (alphaMonitorRunnable != null) {
                alphaMonitorRunnable.setUpdate(true);
            }
        } else if (network == MAIN) {
            if (monitorRunnable != null) {
                monitorRunnable.setUpdate(true);
            }
        }
        for (UUID uuid : MissionControl.getProxies().keySet().stream().filter(uuid -> MissionControl.getProxies().get(uuid).getNetwork() == network).collect(Collectors.toList())) {
            if (MissionControl.getProxies().get(uuid).getNetwork() == network) {
                net.auroramc.proxy.api.backend.communication.ProtocolMessage message = new net.auroramc.proxy.api.backend.communication.ProtocolMessage(net.auroramc.proxy.api.backend.communication.Protocol.UPDATE_MOTD, uuid.toString(), "update", "Mission Control", "&b&lUpdate in progress - Limited network capacity");
                ProxyCommunicationUtils.sendMessage(message);
            }
        }

        if (network == ALPHA) {
            alphaBuilds.putAll(modules);
        } else {
            if (modules.containsKey(Module.CORE)) {
                currentCoreBuildNumber = modules.get(Module.CORE);
                MissionControl.getDbManager().setCurrentCoreBuildNumber(currentCoreBuildNumber);
            }
            if (modules.containsKey(Module.LOBBY)) {
                currentLobbyBuildNumber = modules.get(Module.LOBBY);
                MissionControl.getDbManager().setCurrentLobbyBuildNumber(currentLobbyBuildNumber);
            }
            if (modules.containsKey(Module.BUILD)) {
                currentBuildBuildNumber = modules.get(Module.BUILD);
                MissionControl.getDbManager().setCurrentBuildBuildNumber(currentBuildBuildNumber);
            }
            if (modules.containsKey(Module.ENGINE)) {
                currentEngineBuildNumber = modules.get(Module.ENGINE);
                MissionControl.getDbManager().setCurrentEngineBuildNumber(currentEngineBuildNumber);
            }
            if (modules.containsKey(Module.GAME)) {
                currentGameBuildNumber = modules.get(Module.GAME);
                MissionControl.getDbManager().setCurrentGameBuildNumber(currentGameBuildNumber);
            }
            if (modules.containsKey(Module.PROXY)) {
                currentProxyBuildNumber = modules.get(Module.PROXY);
                MissionControl.getDbManager().setCurrentProxyBuildNumber(currentProxyBuildNumber);
            }
        }

        restarterThread = new NetworkRestarterThread(new ArrayList<>(modules.keySet()), network);
        restarterThread.start();
    }

    public static void updateComplete() {
        ServerInfo.Network network = restarterThread.getNetwork();
        if (network == MAIN) {
            if (monitorRunnable != null) {
                monitorRunnable.setUpdate(false);
            }
        } else if (network == ALPHA) {
            if (alphaMonitorRunnable != null) {
                alphaMonitorRunnable.setUpdate(false);
            }
        }
        restarterThread = null;

        logger.info("Update complete.");

        //Update the MOTD to what it was before.
        for (ProxyInfo uuid : MissionControl.getProxies().values().stream().filter(proxyInfo -> proxyInfo.getNetwork() == network).collect(Collectors.toList())) {
            net.auroramc.proxy.api.backend.communication.ProtocolMessage message = new net.auroramc.proxy.api.backend.communication.ProtocolMessage(net.auroramc.proxy.api.backend.communication.Protocol.UPDATE_MOTD, uuid.getUuid().toString(), "update", "Mission Control", motd.get(MissionControl.getProxies().get(uuid.getUuid()).getNetwork()));
            ProxyCommunicationUtils.sendMessage(message);
        }
    }

    /**
     * Create a connection node. This method blocks until the node has been confirmed to have fully started and be ready to accept connections.
     */
    public static ProxyInfo createProxy(ServerInfo.Network network, boolean forced) {
        UUID uuid = UUID.randomUUID();
        boolean update = false;
        ProxyInfo info = null;
        for (Node node : nodes) {
            if (node.getMemoryLong() - node.getAllocatedMemoryLong() > MemoryAllocation.PROXY.getMegaBytes()) {
                //There is enough memory in this node
                List<Allocation> allocations = node.getAllocations().retrieve().execute().stream().filter(allocation -> !allocation.isAssigned() && Integer.parseInt(allocation.getPort()) < 25660 && !allocation.getIP().equalsIgnoreCase("127.0.0.1")).collect(Collectors.toList());
                if (allocations.size() > 0) {
                    Allocation allocation = allocations.get(0);
                    Allocation protocolAllocation = node.getAllocations().retrieve().execute().stream().filter(allocation1 -> (Integer.parseInt(allocation1.getPort()) == Integer.parseInt(allocation.getPort()) + 100) && !allocation1.getIP().equalsIgnoreCase("127.0.0.1")).collect(Collectors.toList()).get(0);
                    Allocation altProtocolAllocation = node.getAllocations().retrieve().execute().stream().filter(allocation1 -> Integer.parseInt(allocation1.getPort()) == Integer.parseInt(allocation.getPort()) + 100 && allocation1.getIP().equalsIgnoreCase("127.0.0.1")).collect(Collectors.toList()).get(0);
                    info = new ProxyInfo(uuid, allocation.getIP(), allocation.getPortInt(), network, forced, allocation.getPortInt() + 100, (network == ALPHA)?alphaBuilds.get(Module.PROXY):currentProxyBuildNumber);
                    MissionControl.getDbManager().createConnectionNode(info);
                    MissionControl.getPanelManager().createProxy(info, allocation, protocolAllocation, altProtocolAllocation);
                    MissionControl.getProxyManager().addServer(info);
                    MissionControl.getProxies().put(uuid, info);
                    update = true;
                }
            }
        }

        if (update) {
            //Node was found, update the node list.
            nodes = MissionControl.getPanelManager().getAllNodes();
            try {
                proxyBlockingQueue.poll(2, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return info;
        }

        //No nodes were found with enough memory. Ignore request.
        logger.info("A connection node was attempted to be created but a node was not found with enough memory to create it.");
        return null;
    }

    /**
     * Create a connection node. This method blocks until the node has been confirmed to have fully started and be ready to accept connections.
     */
    public static ProxyInfo createProxy(ServerInfo.Network network, boolean forced, int coreBuild, String branch) {
        UUID uuid = UUID.randomUUID();
        boolean update = false;
        ProxyInfo info = null;
        for (Node node : nodes) {
            if (node.getMemoryLong() - node.getAllocatedMemoryLong() > MemoryAllocation.PROXY.getMegaBytes()) {
                //There is enough memory in this node
                List<Allocation> allocations = node.getAllocations().retrieve().execute().stream().filter(allocation -> !allocation.isAssigned() && Integer.parseInt(allocation.getPort()) < 25660 && !allocation.getIP().equalsIgnoreCase("127.0.0.1")).collect(Collectors.toList());
                if (allocations.size() > 0) {
                    Allocation allocation = allocations.get(0);
                    Allocation protocolAllocation = node.getAllocations().retrieve().execute().stream().filter(allocation1 -> (Integer.parseInt(allocation1.getPort()) == Integer.parseInt(allocation.getPort()) + 100) && !allocation1.getIP().equalsIgnoreCase("127.0.0.1")).collect(Collectors.toList()).get(0);
                    Allocation altProtocolAllocation = node.getAllocations().retrieve().execute().stream().filter(allocation1 -> Integer.parseInt(allocation1.getPort()) == Integer.parseInt(allocation.getPort()) + 100 && allocation1.getIP().equalsIgnoreCase("127.0.0.1")).collect(Collectors.toList()).get(0);
                    info = new ProxyInfo(uuid, allocation.getIP(), allocation.getPortInt(), network, forced, allocation.getPortInt() + 100, coreBuild);
                    MissionControl.getDbManager().createConnectionNode(info);
                    MissionControl.getPanelManager().createProxy(info, allocation, protocolAllocation, altProtocolAllocation, branch);
                    MissionControl.getProxyManager().addServer(info);
                    MissionControl.getProxies().put(uuid, info);
                    update = true;
                }
            }
        }

        if (update) {
            //Node was found, update the node list.
            nodes = MissionControl.getPanelManager().getAllNodes();
            try {
                proxyBlockingQueue.poll(2, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return info;
        }

        //No nodes were found with enough memory. Ignore request.
        logger.info("A connection node was attempted to be created but a node was not found with enough memory to create it.");
        return null;
    }

    public static void removeProxyFromRotation(ProxyInfo info) {
        MissionControl.getProxyManager().removeServer(info.getUuid().toString(), info.getNetwork());
    }

    public static void deleteProxy(ProxyInfo info) {
        MissionControl.getPanelManager().closeServer(info.getUuid().toString(), info.getNetwork());
        MissionControl.getPanelManager().deleteServer(info.getUuid().toString(), info.getNetwork());
        MissionControl.getDbManager().deleteNode(info);
        MissionControl.getProxies().remove(info.getUuid());
        networkPlayerTotal.put(info.getNetwork(), networkPlayerTotal.get(info.getNetwork()) - nodePlayerTotals.get(info.getNetwork()).get(info.getUuid()));
        nodePlayerTotals.get(info.getNetwork()).remove(info.getUuid());
        nodes = MissionControl.getPanelManager().getAllNodes();
    }

    public static ServerInfo createServer(String serverName, Game game, boolean forced, ServerInfo.Network network) {
        boolean update = false;

        ServerInfo serverInfo = null;

        int coreBuild = ((network == ALPHA)?alphaBuilds.get(Module.CORE):currentCoreBuildNumber);
        int lobbyBuild = ((game.getModules().contains(Module.LOBBY)?((network == ALPHA)?alphaBuilds.get(Module.LOBBY):currentLobbyBuildNumber):-1));
        int engineBuild = ((game.getModules().contains(Module.ENGINE)?((network == ALPHA)?alphaBuilds.get(Module.ENGINE):currentEngineBuildNumber):-1));
        int gameBuild = ((game.getModules().contains(Module.GAME)?((network == ALPHA)?alphaBuilds.get(Module.GAME):currentGameBuildNumber):-1));
        int buildBuild = ((game.getModules().contains(Module.BUILD)?((network == ALPHA)?alphaBuilds.get(Module.BUILD):currentBuildBuildNumber):-1));

        for (Node node : nodes) {
            if (node.getMemoryLong() - node.getAllocatedMemoryLong() > game.getMemoryAllocation().getMegaBytes()) {
                //There is enough memory in this node
                List<Allocation> allocations = node.getAllocations().retrieve().execute().stream().filter(allocation -> !allocation.isAssigned() && Integer.parseInt(allocation.getPort()) < 25660 && !allocation.getIP().equalsIgnoreCase("127.0.0.1")).collect(Collectors.toList());
                if (allocations.size() > 0) {
                    Allocation allocation = allocations.get(0);
                    Allocation protocolAllocation = node.getAllocations().retrieve().execute().stream().filter(allocation1 -> (Integer.parseInt(allocation1.getPort()) == Integer.parseInt(allocation.getPort()) + 100) && !allocation1.getIP().equalsIgnoreCase("127.0.0.1")).collect(Collectors.toList()).get(0);
                    Allocation altAllocation = node.getAllocations().retrieve().execute().stream().filter(allocation1 -> Integer.parseInt(allocation1.getPort()) == Integer.parseInt(allocation.getPort()) && allocation1.getIP().equalsIgnoreCase("127.0.0.1")).collect(Collectors.toList()).get(0);
                    Allocation altProtocolAllocation = node.getAllocations().retrieve().execute().stream().filter(allocation1 -> Integer.parseInt(allocation1.getPort()) == Integer.parseInt(allocation.getPort()) + 100 && allocation1.getIP().equalsIgnoreCase("127.0.0.1")).collect(Collectors.toList()).get(0);
                    serverInfo = new ServerInfo(serverName, allocation.getIP(), allocation.getPortInt(), network, forced, game.getServerTypeInformation(), allocation.getPortInt() + 100, coreBuild, lobbyBuild, engineBuild, gameBuild, buildBuild);
                    MissionControl.getDbManager().createServer(serverInfo);
                    MissionControl.getPanelManager().createServer(serverInfo, game.getMemoryAllocation(), allocation, protocolAllocation, altAllocation, altProtocolAllocation);
                    MissionControl.getServers().get(network).put(serverName, serverInfo);
                    update = true;
                }
            }
        }

        if (update) {
            //Node was found, update the node list.
            nodes = MissionControl.getPanelManager().getAllNodes();
            try {
                ServerInfo server = serverBlockingQueue.poll(2, TimeUnit.MINUTES);
                if (server != null) {
                    if (!server.getServerType().getString("type").equalsIgnoreCase("lobby") && !server.getServerType().getString("type").equalsIgnoreCase("build") && !server.getServerType().getString("type").equalsIgnoreCase("staff")) {
                        //Send a message to all lobbies on that network that there is a new server online
                        List<ServerInfo> infos = MissionControl.getServers().get(network).values().stream().filter(info -> info.getServerType().getString("type").equalsIgnoreCase("lobby")).collect(Collectors.toList());
                        for (ServerInfo info : infos) {
                            ProtocolMessage message = new ProtocolMessage(Protocol.SERVER_ONLINE ,info.getName(), "add", "Mission Control", server.getName());
                            ServerCommunicationUtils.sendMessage(message, network);
                        }

                    }
                    for (ProxyInfo info : MissionControl.getProxies().values().stream().filter(proxyInfo -> proxyInfo.getNetwork() == network).collect(Collectors.toList())) {
                        net.auroramc.proxy.api.backend.communication.ProtocolMessage message = new net.auroramc.proxy.api.backend.communication.ProtocolMessage(net.auroramc.proxy.api.backend.communication.Protocol.SERVER_ONLINE, info.getUuid().toString(), "add", "Mission Control", server.getName());
                        ProxyCommunicationUtils.sendMessage(message);
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return serverInfo;
        }

        //No nodes were found with enough memory. Ignore request.
        logger.info("A server was attempted to be created but a node was not found with enough memory to create it.");
        return null;
    }

    public static ServerInfo createServer(String serverName, Game game, boolean forced, ServerInfo.Network network, int coreBuild, String coreBranch, int lobbyBuild, String lobbybranch, int buildBuild, String buildBranch, int gameBuild, String gameBranch, int engineBuild, String engineBranch) {
        boolean update = false;

        ServerInfo serverInfo = null;

        for (Node node : nodes) {
            if (node.getMemoryLong() - node.getAllocatedMemoryLong() > game.getMemoryAllocation().getMegaBytes()) {
                //There is enough memory in this node
                List<Allocation> allocations = node.getAllocations().retrieve().execute().stream().filter(allocation -> !allocation.isAssigned() && Integer.parseInt(allocation.getPort()) < 25660 && !allocation.getIP().equalsIgnoreCase("127.0.0.1")).collect(Collectors.toList());
                if (allocations.size() > 0) {
                    Allocation allocation = allocations.get(0);
                    Allocation protocolAllocation = node.getAllocations().retrieve().execute().stream().filter(allocation1 -> (Integer.parseInt(allocation1.getPort()) == Integer.parseInt(allocation.getPort()) + 100) && !allocation1.getIP().equalsIgnoreCase("127.0.0.1")).collect(Collectors.toList()).get(0);
                    Allocation altAllocation = node.getAllocations().retrieve().execute().stream().filter(allocation1 -> Integer.parseInt(allocation1.getPort()) == Integer.parseInt(allocation.getPort()) && allocation1.getIP().equalsIgnoreCase("127.0.0.1")).collect(Collectors.toList()).get(0);
                    Allocation altProtocolAllocation = node.getAllocations().retrieve().execute().stream().filter(allocation1 -> Integer.parseInt(allocation1.getPort()) == Integer.parseInt(allocation.getPort()) + 100 && allocation1.getIP().equalsIgnoreCase("127.0.0.1")).collect(Collectors.toList()).get(0);
                    serverInfo = new ServerInfo(serverName, allocation.getIP(), allocation.getPortInt(), network, forced, game.getServerTypeInformation(), allocation.getPortInt() + 100, coreBuild, lobbyBuild, engineBuild, gameBuild, buildBuild);
                    MissionControl.getDbManager().createServer(serverInfo);
                    MissionControl.getPanelManager().createServer(serverInfo, game.getMemoryAllocation(), allocation, protocolAllocation, altAllocation, altProtocolAllocation, coreBranch, lobbybranch, buildBranch, gameBranch, engineBranch);
                    MissionControl.getServers().get(network).put(serverName, serverInfo);
                    update = true;
                }
            }
        }

        if (update) {
            //Node was found, update the node list.
            nodes = MissionControl.getPanelManager().getAllNodes();
            try {
                ServerInfo server = serverBlockingQueue.poll(2, TimeUnit.MINUTES);
                if (server != null) {
                    if (!server.getServerType().getString("type").equalsIgnoreCase("lobby") && !server.getServerType().getString("type").equalsIgnoreCase("build") && !server.getServerType().getString("type").equalsIgnoreCase("staff")) {
                        //Send a message to all lobbies on that network that there is a new server online
                        List<ServerInfo> infos = MissionControl.getServers().get(network).values().stream().filter(info -> info.getServerType().getString("type").equalsIgnoreCase("lobby")).collect(Collectors.toList());
                        for (ServerInfo info : infos) {
                            ProtocolMessage message = new ProtocolMessage(Protocol.SERVER_ONLINE ,info.getName(), "add", "Mission Control", server.getName());
                            ServerCommunicationUtils.sendMessage(message, network);
                        }
                        for (ProxyInfo info : MissionControl.getProxies().values().stream().filter(proxyInfo -> proxyInfo.getNetwork() == network).collect(Collectors.toList())) {
                            net.auroramc.proxy.api.backend.communication.ProtocolMessage message = new net.auroramc.proxy.api.backend.communication.ProtocolMessage(net.auroramc.proxy.api.backend.communication.Protocol.SERVER_ONLINE, info.getUuid().toString(), "remove", "Mission Control", server.getName());
                            ProxyCommunicationUtils.sendMessage(message);
                        }
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return serverInfo;
        }

        //No nodes were found with enough memory. Ignore request.
        logger.info("A server was attempted to be created but a node was not found with enough memory to create it.");
        return null;
    }

    public static void removeServerFromRotation(ServerInfo server) {
        List<ServerInfo> infos = MissionControl.getServers().get(server.getNetwork()).values().stream().filter(info -> info.getServerType().getString("type").equalsIgnoreCase("lobby")).collect(Collectors.toList());
        for (ServerInfo info : infos) {
            ProtocolMessage message = new ProtocolMessage(Protocol.REMOVE_SERVER ,info.getName(), "remove", "Mission Control", server.getName());
            ServerCommunicationUtils.sendMessage(message, info.getNetwork());
        }
        for (ProxyInfo info : MissionControl.getProxies().values().stream().filter(proxyInfo -> proxyInfo.getNetwork() == server.getNetwork()).collect(Collectors.toList())) {
            net.auroramc.proxy.api.backend.communication.ProtocolMessage message = new net.auroramc.proxy.api.backend.communication.ProtocolMessage(net.auroramc.proxy.api.backend.communication.Protocol.SERVER_OFFLINE, info.getUuid().toString(), "remove", "Mission Control", server.getName());
            ProxyCommunicationUtils.sendMessage(message);
        }
    }

    public static void closeServer(ServerInfo info) {
        MissionControl.getPanelManager().deleteServer(info.getName(), info.getNetwork());
        MissionControl.getDbManager().deleteServer(info);
        MissionControl.getServers().get(info.getNetwork()).remove(info.getName());
        serverPlayerTotals.get(info.getNetwork()).remove(info.getName());
        nodes = MissionControl.getPanelManager().getAllNodes();
    }

    public static synchronized void proxyOpenConfirmation(ProxyInfo info) {
        proxyBlockingQueue.add(info);
    }

    public static synchronized void serverOpenConfirmation(ServerInfo info) {
        serverBlockingQueue.add(info);
    }

    public static void shutdown() {
        logger.info("Shutting down scheduler...");
        scheduler.shutdown();
        logger.info("Shutting down protocol threads...");
        ProxyCommunicationUtils.shutdown();
        ServerCommunicationUtils.shutdown();
        logger.info("Shutdown complete. Goodbye!");
        System.exit(0);
    }

    public static void playerJoinedNetwork(UUID proxy, ServerInfo.Network network) {
        synchronized (lock) {
            networkPlayerTotal.put(network, networkPlayerTotal.get(network) + 1);
            if (nodePlayerTotals.get(network).containsKey(proxy)) {
                nodePlayerTotals.get(network).put(proxy, nodePlayerTotals.get(network).get(proxy) + 1);
            } else {
                nodePlayerTotals.get(network).put(proxy, 1);
            }
        }
    }

    public static void playerLeftNetwork(UUID proxy, ServerInfo.Network network) {
        synchronized (lock) {
            networkPlayerTotal.put(network, networkPlayerTotal.get(network) - 1);
            if (nodePlayerTotals.get(network).containsKey(proxy)) {
                int newTotal = nodePlayerTotals.get(network).get(proxy) - 1;
                if (newTotal <= 0) {
                    nodePlayerTotals.get(network).remove(proxy);
                    return;
                }
                nodePlayerTotals.get(network).put(proxy, newTotal);
            }
        }
    }

    public static void playerJoinedServer(String newServer, Game game, ServerInfo.Network network) {
        synchronized (lock) {
            if (serverPlayerTotals.get(network).containsKey(newServer)) {
                serverPlayerTotals.get(network).put(newServer, serverPlayerTotals.get(network).get(newServer) + 1);
            } else {
                serverPlayerTotals.get(network).put(newServer, 1);
            }
            if (game != null) {
                gamePlayerTotals.get(network).put(game, gamePlayerTotals.get(network).get(game) + 1);
            }
        }
    }

    public static void playerLeftServer(String oldServer, Game game, ServerInfo.Network network) {
        synchronized (lock) {
            if (serverPlayerTotals.get(network).containsKey(oldServer)) {
                int newTotal = serverPlayerTotals.get(network).get(oldServer) - 1;
                if (newTotal <= 0) {
                    serverPlayerTotals.get(network).remove(oldServer);
                    return;
                }
                serverPlayerTotals.get(network).put(oldServer, newTotal);
            }
            if (game != null) {
                gamePlayerTotals.get(network).put(game, gamePlayerTotals.get(network).get(game) - 1);
            }
        }
    }

    public static void reportServerTotal(String server, Game game, int amount, ServerInfo.Network network) {
        synchronized (lock) {
            if (serverPlayerTotals.get(network).containsKey(server)) {
                gamePlayerTotals.get(network).put(game, gamePlayerTotals.get(network).get(game) - serverPlayerTotals.get(network).get(server));
            }
            serverPlayerTotals.get(network).put(server, amount);
            if (game != null) {
                gamePlayerTotals.get(network).put(game, gamePlayerTotals.get(network).get(game) + amount);
            }
        }
    }

    public static void reportProxyTotal(UUID proxy, int amount, ServerInfo.Network network) {
        synchronized (lock) {
            if (nodePlayerTotals.get(network).containsKey(proxy)) {
                networkPlayerTotal.put(network, networkPlayerTotal.get(network) - nodePlayerTotals.get(network).get(proxy));
            }
            nodePlayerTotals.get(network).put(proxy, amount);
            networkPlayerTotal.put(network, networkPlayerTotal.get(network) + amount);
        }
    }

    public static void proxyClose(UUID proxy, ServerInfo.Network network) {
        synchronized (lock) {
            if (nodePlayerTotals.get(network).containsKey(proxy)) {
                networkPlayerTotal.put(network, networkPlayerTotal.get(network) - nodePlayerTotals.get(network).get(proxy));
            }
            nodePlayerTotals.get(network).remove(proxy);
        }
    }

    public static void serverClose(String server, Game game, ServerInfo.Network network) {
        synchronized (lock) {
            if (serverPlayerTotals.get(network).containsKey(server)) {
                gamePlayerTotals.get(network).put(game, gamePlayerTotals.get(network).get(game) - serverPlayerTotals.get(network).get(server));
            }
            serverPlayerTotals.get(network).remove(server);
        }
    }

    public static Map<ServerInfo.Network, Integer> getNetworkPlayerTotal() {
        return networkPlayerTotal;
    }

    public static Map<ServerInfo.Network, Map<Game, Integer>> getGamePlayerTotals() {
        return gamePlayerTotals;
    }

    public static Map<ServerInfo.Network, Map<String, Integer>> getServerPlayerTotals() {
        return serverPlayerTotals;
    }

    public static Map<ServerInfo.Network, Map<UUID, Integer>> getNodePlayerTotals() {
        return nodePlayerTotals;
    }

    public static int getCurrentBuildBuildNumber() {
        return currentBuildBuildNumber;
    }

    public static int getCurrentCoreBuildNumber() {
        return currentCoreBuildNumber;
    }

    public static int getCurrentEngineBuildNumber() {
        return currentEngineBuildNumber;
    }

    public static int getCurrentGameBuildNumber() {
        return currentGameBuildNumber;
    }

    public static int getCurrentLobbyBuildNumber() {
        return currentLobbyBuildNumber;
    }

    public static int getCurrentProxyBuildNumber() {
        return currentProxyBuildNumber;
    }

    public static NetworkMonitorRunnable getAlphaMonitorRunnable() {
        return alphaMonitorRunnable;
    }

    public static NetworkMonitorRunnable getMonitorRunnable() {
        return monitorRunnable;
    }

    public static NetworkRestarterThread getRestarterThread() {
        return restarterThread;
    }

    public static boolean isAlphaEnabled() {
        return alphaEnabled;
    }

    public static boolean isServerMonitoringEnabled(ServerInfo.Network network) {
        return serverMonitorEnabled.get(network);
    }

    public static boolean isGameEnabled(Game game, ServerInfo.Network network) {
        return gameEnabled.get(network).get(game);
    }

    public static boolean isGameMonitored(Game game, ServerInfo.Network network) {
        return gameMonitor.get(network).get(game);
    }

    public static void setAlphaEnabled(boolean enabled) {
        alphaEnabled = enabled;
        dbManager.setAlphaEnabled(enabled);
        if (enabled) {
            if (alphaMonitorRunnable != null) {
                alphaMonitorRunnable.setEnabled(true);
                return;
            }
            alphaMonitorRunnable = new NetworkMonitorRunnable(logger, ALPHA);
            scheduler.scheduleWithFixedDelay(alphaMonitorRunnable, 1, 1, TimeUnit.MINUTES);
        } else {
            alphaMonitorRunnable.setEnabled(false);
            Collection<ServerInfo> infos = MissionControl.getServers().get(ALPHA).values();
            for (ServerInfo info : infos) {
                ProtocolMessage message = new ProtocolMessage(Protocol.ALPHA_CHANGE ,info.getName(), "disable", "Mission Control", "");
                ServerCommunicationUtils.sendMessage(message, info.getNetwork());
            }
            for (ProxyInfo info : MissionControl.getProxies().values().stream().filter(proxyInfo -> proxyInfo.getNetwork() == ALPHA).collect(Collectors.toList())) {
                net.auroramc.proxy.api.backend.communication.ProtocolMessage message = new net.auroramc.proxy.api.backend.communication.ProtocolMessage(net.auroramc.proxy.api.backend.communication.Protocol.ALPHA_UPDATE, info.getUuid().toString(), "disable", "Mission Control", "");
                ProxyCommunicationUtils.sendMessage(message);
            }
        }
    }

    public static void setServerManagerEnabled(ServerInfo.Network network, boolean enabled) {
        if (network == TEST) return;
        serverMonitorEnabled.put(network, enabled);
        dbManager.setServerManagerEnabled(network, enabled);
        if (enabled && network == ALPHA) {
            if (alphaMonitorRunnable != null) {
                alphaMonitorRunnable.setEnabled(true);
                return;
            }
            alphaMonitorRunnable = new NetworkMonitorRunnable(logger, ALPHA);
            scheduler.scheduleWithFixedDelay(alphaMonitorRunnable, 1, 1, TimeUnit.MINUTES);
        } else if (enabled && network == MAIN) {
            if (monitorRunnable != null) {
                monitorRunnable.setEnabled(true);
                return;
            }
            monitorRunnable = new NetworkMonitorRunnable(logger, MAIN);
            scheduler.scheduleWithFixedDelay(monitorRunnable, 1, 1, TimeUnit.MINUTES);
        } else {
            if (network == MAIN) {
                monitorRunnable.setEnabled(false);
                return;
            }
            alphaMonitorRunnable.setEnabled(false);
        }
    }

    public static boolean isUpdate() {
        return restarterThread != null;
    }

    public static void setMotd(ServerInfo.Network network, String motd) {
        NetworkManager.motd.put(network, motd);
    }

    public static void setMaintenanceMotd(ServerInfo.Network network, String motd) {
        NetworkManager.maintenanceMotd.put(network, motd);
    }

    public static void setMaintenance(ServerInfo.Network network, boolean maintenance) {
        NetworkManager.maintenance.put(network, maintenance);
    }

    public static void setMaintenanceMode(ServerInfo.Network network, MaintenanceMode mode) {
        NetworkManager.maintenanceMode.put(network, mode);
    }

    public static Map<Module, Integer> getAlphaBuilds() {
        return alphaBuilds;
    }

    public static Map<Module, String> getAlphaBranches() {
        return alphaBranches;
    }

    public static void enableGame(Game game, ServerInfo.Network network) {
        if (network == null) {
            for (ServerInfo.Network net : ServerInfo.Network.values()) {
                gameEnabled.get(net).put(game, true);
                dbManager.setGameEnabled(game, net, true);
            }
        } else {
            gameEnabled.get(network).put(game, true);
            dbManager.setGameEnabled(game, network, true);
        }
    }

    public static void disableGame(Game game, ServerInfo.Network network) {
        if (network == null) {
            for (ServerInfo.Network net : ServerInfo.Network.values()) {
                gameEnabled.get(net).put(game, false);
                dbManager.setGameEnabled(game, net, false);
                Collection<ServerInfo> infos = MissionControl.getServers().get(net).values().stream().filter(serverInfo -> serverInfo.getServerType().getString("game").equalsIgnoreCase(game.name())).collect(Collectors.toList());
                for (ServerInfo info : infos) {
                    ProtocolMessage message = new ProtocolMessage(Protocol.SHUTDOWN ,info.getName(), "shutdown", "Mission Control", "");
                    ServerCommunicationUtils.sendMessage(message, info.getNetwork());
                }
            }
        } else {
            gameEnabled.get(network).put(game, false);
            dbManager.setGameEnabled(game, network, false);
        }
    }

    public static void setMonitored(Game game, ServerInfo.Network network, boolean monitored) {
        if (network == null) {
            for (ServerInfo.Network net : ServerInfo.Network.values()) {
                gameMonitor.get(net).put(game, monitored);
                dbManager.setMonitoringEnabled(game, net, monitored);
            }
        } else {
            gameMonitor.get(network).put(game, monitored);
            dbManager.setMonitoringEnabled(game, network, monitored);
        }
    }
}
