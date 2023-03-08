/*
 * Copyright (c) 2021 AuroraMC Ltd. All Rights Reserved.
 */

package net.auroramc.missioncontrol;

import com.mattmalec.pterodactyl4j.application.entities.ApplicationAllocation;
import com.mattmalec.pterodactyl4j.application.entities.Node;
import net.auroramc.core.api.backend.communication.Protocol;
import net.auroramc.core.api.backend.communication.ProtocolMessage;
import net.auroramc.core.api.backend.communication.ServerCommunicationUtils;
import net.auroramc.missioncontrol.backend.communication.panel.PanelCommunicationUtils;
import net.auroramc.missioncontrol.backend.managers.DatabaseManager;
import net.auroramc.missioncontrol.backend.runnables.PlayerCountUpdateRunnable;
import net.auroramc.missioncontrol.backend.runnables.RequestPlayerCountUpdateRunnable;
import net.auroramc.missioncontrol.backend.runnables.StatUpdateRunnable;
import net.auroramc.missioncontrol.backend.runnables.StoreCommandProcessRunnable;
import net.auroramc.missioncontrol.backend.store.PaymentProcessor;
import net.auroramc.missioncontrol.backend.store.packages.bundles.Celebration;
import net.auroramc.missioncontrol.backend.store.packages.bundles.Starter;
import net.auroramc.missioncontrol.backend.store.packages.crates.Diamond5;
import net.auroramc.missioncontrol.backend.store.packages.crates.Gold5;
import net.auroramc.missioncontrol.backend.store.packages.crates.Iron5;
import net.auroramc.missioncontrol.backend.store.packages.plus.Plus180;
import net.auroramc.missioncontrol.backend.store.packages.plus.Plus30;
import net.auroramc.missioncontrol.backend.store.packages.plus.Plus365;
import net.auroramc.missioncontrol.backend.store.packages.plus.Plus90;
import net.auroramc.missioncontrol.backend.store.packages.ranks.Elite;
import net.auroramc.missioncontrol.backend.store.packages.ranks.Master;
import net.auroramc.missioncontrol.backend.util.*;
import net.auroramc.missioncontrol.entities.ProxyInfo;
import net.auroramc.missioncontrol.entities.ServerInfo;
import net.auroramc.proxy.api.backend.communication.ProxyCommunicationUtils;
import net.buycraft.plugin.BuyCraftAPI;
import org.apache.commons.lang3.RandomStringUtils;
import org.json.JSONObject;

import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
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
    private static int currentDuelsBuildNumber;
    private static int currentPathfinderBuildNumber;

    private static final Logger logger;
    private static final DatabaseManager dbManager;
    private static final ScheduledExecutorService scheduler;

    private static Map<ServerInfo.Network, String> motd;
    private static Map<ServerInfo.Network, Boolean> maintenance;
    private static Map<ServerInfo.Network, MaintenanceMode> maintenanceMode;
    private static Map<ServerInfo.Network, String> maintenanceMotd;

    private static List<Node> nodes;
    private static NetworkRestarterThread serverRestarterThread;
    private static ProxyRestarterThread proxyRestarterThread;
    private static NetworkMonitorRunnable monitorRunnable;
    private static NetworkMonitorRunnable alphaMonitorRunnable;

    private static Map<ServerInfo.Network, Map<ServerType, Boolean>> gameEnabled;
    private static Map<ServerInfo.Network, Map<ServerType, Boolean>> gameMonitor;

    private static Map<Module, String> alphaBranches;
    private static Map<Module, Integer> alphaBuilds;

    private static final Map<ServerInfo.Network, Boolean> serverMonitorEnabled;
    private static boolean alphaEnabled;

    static {

        logger = MissionControl.getLogger();
        dbManager = MissionControl.getDbManager();

        serverMonitorEnabled = new HashMap<>();

        for (ServerInfo.Network network : ServerInfo.Network.values()) {
            if (network == TEST) continue;
            serverMonitorEnabled.put(network, dbManager.isServerManagerEnabled(network));

        }

        nodes = MissionControl.getPanelManager().getAllNodes();

        scheduler = Executors.newScheduledThreadPool(9);
        shutdown = false;
    }


    public static void handoff(String storeApiKey) {
        logger.info("Fetching pushed builds...");
        currentCoreBuildNumber = dbManager.getCurrentCoreBuildNumber();
        currentBuildBuildNumber = dbManager.getCurrentBuildBuildNumber();
        currentEngineBuildNumber = dbManager.getCurrentEngineBuildNumber();
        currentGameBuildNumber = dbManager.getCurrentGameBuildNumber();
        currentLobbyBuildNumber = dbManager.getCurrentLobbyBuildNumber();
        currentProxyBuildNumber = dbManager.getCurrentProxyBuildNumber();
        currentDuelsBuildNumber = dbManager.getCurrentDuelsBuildNumber();
        currentDuelsBuildNumber = dbManager.getCurrentPathfinderBuildNumber();

        motd = dbManager.getMotd();
        maintenance = dbManager.getMaintenance();
        maintenanceMode = dbManager.getMaintenanceMode();
        maintenanceMotd = dbManager.getMaintenanceMotd();

        alphaBranches = dbManager.getBranchMappings();
        alphaBuilds = dbManager.getBuildMappings();

        //Load in all known store packages.
        PaymentProcessor.registerPackage("elite", new Elite());
        PaymentProcessor.registerPackage("master", new Master());
        PaymentProcessor.registerPackage("plus30", new Plus30());
        PaymentProcessor.registerPackage("plus90", new Plus90());
        PaymentProcessor.registerPackage("plus180", new Plus180());
        PaymentProcessor.registerPackage("plus365", new Plus365());
        PaymentProcessor.registerPackage("celebration", new Celebration());
        PaymentProcessor.registerPackage("starter", new Starter());
        PaymentProcessor.registerPackage("iron5", new Iron5());
        PaymentProcessor.registerPackage("gold5", new Gold5());
        PaymentProcessor.registerPackage("diamond5", new Diamond5());

        gameEnabled = new HashMap<>();
        for (ServerInfo.Network network : ServerInfo.Network .values()) {
            gameEnabled.put(network, dbManager.getGameEnabled(network));
        }

        gameMonitor = new HashMap<>();
        for (ServerInfo.Network network : ServerInfo.Network .values()) {
            gameMonitor.put(network, dbManager.getMonitoring(network));
        }

        if (MissionControl.isClean()) {
            logger.info("Restarting all servers...");
            Map<Module, Integer> modules = new HashMap<>();
            modules.put(Module.PROXY, currentProxyBuildNumber);
            modules.put(Module.CORE, currentCoreBuildNumber);
            pushUpdate(modules, MAIN);
            logger.info("All servers queued for restart, starting network monitoring thread...");
        } else {
            logger.info("Mission Control did not restart cleanly so servers were not queued for restart. Starting network monitoring thread...");
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
        scheduler.scheduleWithFixedDelay(new RequestPlayerCountUpdateRunnable(), 1, 1, TimeUnit.MINUTES);

        //Statistics updater
        scheduler.scheduleWithFixedDelay(new StatUpdateRunnable(StatUpdateRunnable.StatisticPeriod.DAILY), 0, 10, TimeUnit.MINUTES);
        scheduler.scheduleWithFixedDelay(new StatUpdateRunnable(StatUpdateRunnable.StatisticPeriod.WEEKLY), 0, 1, TimeUnit.HOURS);
        scheduler.schedule(() -> {
            new StatUpdateRunnable(StatUpdateRunnable.StatisticPeriod.ALLTIME).run();
            MissionControl.getLogger().info("Performing Mission Control Restart...");
            interrupt();
        }, 1,TimeUnit.DAYS);

        BuyCraftAPI api = BuyCraftAPI.create(storeApiKey);
        scheduler.scheduleWithFixedDelay(new StoreCommandProcessRunnable(api), 0, 10, TimeUnit.MINUTES);

        done();
    }

    private static void done() {
        logger.info("Handoff complete.");
        String command;
        try {
            while (!shutdown) {
                synchronized (lock3) {
                    //Add a timeout in-case interrupting didnt work. Checks once every hour.
                    lock3.wait(3600000L);
                }
            }
        } catch (InterruptedException e) {
            logger.log(Level.INFO, "Interrupt received, shutting down.", e);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "An error has occurred while trying to process commands. Shutting down. Stack trace: ", e);
        }
        logger.info("Shutting down...");
        shutdown();
    }

    public static void interrupt() {
        shutdown = true;
        lock3.notifyAll();
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
            if (modules.containsKey(Module.DUELS)) {
                currentDuelsBuildNumber = modules.get(Module.DUELS);
                MissionControl.getDbManager().setCurrentDuelsBuildNumber(currentDuelsBuildNumber);
            }
            if (modules.containsKey(Module.PATHFINDER)) {
                currentPathfinderBuildNumber = modules.get(Module.PATHFINDER);
                MissionControl.getDbManager().setCurrentPathfinderBuildNumber(currentPathfinderBuildNumber);
            }
        }
        if (modules.containsKey(Module.PROXY)) {
            proxyRestarterThread = new ProxyRestarterThread(network);
            proxyRestarterThread.start();
        }
        if (modules.size() > 1 || !modules.containsKey(Module.PROXY)) {
            serverRestarterThread = new NetworkRestarterThread(new ArrayList<>(modules.keySet()), network);
            serverRestarterThread.start();
        }
    }

    public static void updateComplete() {
        ServerInfo.Network network = serverRestarterThread.getNetwork();
        serverRestarterThread = null;
        if (network == MAIN) {
            if (monitorRunnable != null && proxyRestarterThread == null) {
                monitorRunnable.setUpdate(false);
            }
        } else if (network == ALPHA) {
            if (alphaMonitorRunnable != null && proxyRestarterThread == null) {
                alphaMonitorRunnable.setUpdate(false);
            }
        }

        logger.info("Update complete.");

        //Update the MOTD to what it was before.
        for (ProxyInfo uuid : MissionControl.getProxies().values().stream().filter(proxyInfo -> proxyInfo.getNetwork() == network).collect(Collectors.toList())) {
            net.auroramc.proxy.api.backend.communication.ProtocolMessage message = new net.auroramc.proxy.api.backend.communication.ProtocolMessage(net.auroramc.proxy.api.backend.communication.Protocol.UPDATE_MOTD, uuid.getUuid().toString(), "update", "Mission Control", motd.get(MissionControl.getProxies().get(uuid.getUuid()).getNetwork()));
            ProxyCommunicationUtils.sendMessage(message);
        }
    }

    public static void proxyUpdateComplete() {
        ServerInfo.Network network = proxyRestarterThread.getNetwork();
        proxyRestarterThread = null;
        if (network == MAIN) {
            if (monitorRunnable != null && serverRestarterThread == null) {
                monitorRunnable.setUpdate(false);
            }
        } else if (network == ALPHA) {
            if (alphaMonitorRunnable != null && serverRestarterThread == null) {
                alphaMonitorRunnable.setUpdate(false);
            }
        }

        logger.info("Proxy update complete.");

        //Update the MOTD to what it was before.
        for (ProxyInfo uuid : MissionControl.getProxies().values().stream().filter(proxyInfo -> proxyInfo.getNetwork() == network).collect(Collectors.toList())) {
            net.auroramc.proxy.api.backend.communication.ProtocolMessage message = new net.auroramc.proxy.api.backend.communication.ProtocolMessage(net.auroramc.proxy.api.backend.communication.Protocol.UPDATE_MOTD, uuid.getUuid().toString(), "update", "Mission Control", motd.get(MissionControl.getProxies().get(uuid.getUuid()).getNetwork()));
            ProxyCommunicationUtils.sendMessage(message);
        }
    }

    /**
     * Create a connection node. This method blocks until the node has been confirmed to have fully started and be ready to accept connections.
     */
    public static ProxyInfo createProxy(ServerInfo.Network network, boolean forced, boolean block) {
        UUID uuid = UUID.randomUUID();
        boolean update = false;
        ProxyInfo info = null;
        for (Node node : nodes) {
            if (node.getMemoryLong() - node.getAllocatedMemoryLong() > MemoryAllocation.PROXY.getMegaBytes()) {
                //There is enough memory in this node
                List<ApplicationAllocation> allocations = node.retrieveAllocations().all().execute().stream().filter(allocation -> !allocation.isAssigned() && Integer.parseInt(allocation.getPort()) < 25660 && !allocation.getIP().equalsIgnoreCase("127.0.0.1")).collect(Collectors.toList());
                if (allocations.size() > 0) {
                    ApplicationAllocation allocation = allocations.get(0);
                    ApplicationAllocation protocolAllocation = node.retrieveAllocations().all().execute().stream().filter(allocation1 -> (Integer.parseInt(allocation1.getPort()) == Integer.parseInt(allocation.getPort()) + 100) && !allocation1.getIP().equalsIgnoreCase("127.0.0.1")).collect(Collectors.toList()).get(0);
                    ApplicationAllocation altProtocolAllocation = node.retrieveAllocations().all().execute().stream().filter(allocation1 -> Integer.parseInt(allocation1.getPort()) == Integer.parseInt(allocation.getPort()) + 100 && allocation1.getIP().equalsIgnoreCase("127.0.0.1")).collect(Collectors.toList()).get(0);
                    String authKey = RandomStringUtils.randomAscii(36);
                    info = new ProxyInfo(uuid, allocation.getIP(), allocation.getPortInt(), network, forced, allocation.getPortInt() + 100, (network == ALPHA)?alphaBuilds.get(Module.PROXY):currentProxyBuildNumber, authKey);
                    MissionControl.getDbManager().createConnectionNode(info);
                    MissionControl.getPanelManager().createProxy(info, allocation, protocolAllocation, altProtocolAllocation);
                    MissionControl.getProxies().put(uuid, info);
                    update = true;
                }
            }
        }

        if (update) {
            //Node was found, update the node list.
            nodes = MissionControl.getPanelManager().getAllNodes();
            if (block) {
                try {
                    proxyBlockingQueue.poll(2, TimeUnit.MINUTES);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                new Thread(() -> {
                    try {
                        proxyBlockingQueue.poll(2, TimeUnit.MINUTES);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }).start();
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
    public static ProxyInfo createProxy(ServerInfo.Network network, boolean forced, int coreBuild, String branch, boolean block) {
        UUID uuid = UUID.randomUUID();
        boolean update = false;
        ProxyInfo info = null;
        for (Node node : nodes) {
            if (node.getMemoryLong() - node.getAllocatedMemoryLong() > MemoryAllocation.PROXY.getMegaBytes()) {
                //There is enough memory in this node
                List<ApplicationAllocation> allocations = node.retrieveAllocations().all().execute().stream().filter(allocation -> !allocation.isAssigned() && Integer.parseInt(allocation.getPort()) < 25660 && !allocation.getIP().equalsIgnoreCase("127.0.0.1")).collect(Collectors.toList());
                if (allocations.size() > 0) {
                    ApplicationAllocation allocation = allocations.get(0);
                    ApplicationAllocation protocolAllocation = node.retrieveAllocations().all().execute().stream().filter(allocation1 -> (Integer.parseInt(allocation1.getPort()) == Integer.parseInt(allocation.getPort()) + 100) && !allocation1.getIP().equalsIgnoreCase("127.0.0.1")).collect(Collectors.toList()).get(0);
                    ApplicationAllocation altProtocolAllocation = node.retrieveAllocations().all().execute().stream().filter(allocation1 -> Integer.parseInt(allocation1.getPort()) == Integer.parseInt(allocation.getPort()) + 100 && allocation1.getIP().equalsIgnoreCase("127.0.0.1")).collect(Collectors.toList()).get(0);
                    String authKey = RandomStringUtils.randomAscii(36);
                    info = new ProxyInfo(uuid, allocation.getIP(), allocation.getPortInt(), network, forced, allocation.getPortInt() + 100, coreBuild, authKey);
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
            if (block) {
                try {
                    proxyBlockingQueue.poll(2, TimeUnit.MINUTES);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                new Thread(() -> {
                    try {
                        proxyBlockingQueue.poll(2, TimeUnit.MINUTES);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }).start();
            }
            return info;
        }

        //No nodes were found with enough memory. Ignore request.
        logger.info("A connection node was attempted to be created but a node was not found with enough memory to create it.");
        return null;
    }

    public static void waitForProxyResponse() {
        try {
            proxyBlockingQueue.poll(2, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void removeProxyFromRotation(ProxyInfo info) {
        MissionControl.getProxyManager().removeServer(info.getUuid().toString(), info.getNetwork());
    }

    public static void deleteProxy(ProxyInfo info) {
        MissionControl.getPanelManager().closeServer(info.getUuid().toString(), info.getNetwork());
        MissionControl.getPanelManager().deleteServer(info.getUuid().toString(), info.getNetwork());
        MissionControl.getDbManager().deleteNode(info);
        MissionControl.getProxies().remove(info.getUuid());
        nodes = MissionControl.getPanelManager().getAllNodes();
    }

    public static ServerInfo createPathfinderServer(String serverName, JSONObject serverType, boolean forced, ServerInfo.Network network, boolean block) {
        boolean update = false;

        ServerInfo serverInfo = null;

        int coreBuild = ((network == ALPHA)?alphaBuilds.get(Module.CORE):currentCoreBuildNumber);
        int pathfinderBuild = ((network == ALPHA)?alphaBuilds.get(Module.PATHFINDER):currentPathfinderBuildNumber);

        for (Node node : nodes) {
            if (node.getMemoryLong() - node.getAllocatedMemoryLong() > serverType.getInt("memory_allocation")) {
                //There is enough memory in this node
                List<ApplicationAllocation> allocations = node.retrieveAllocations().all().execute().stream().filter(allocation -> !allocation.isAssigned() && Integer.parseInt(allocation.getPort()) < 25660 && !allocation.getIP().equalsIgnoreCase("127.0.0.1")).collect(Collectors.toList());
                if (allocations.size() > 0) {
                    ApplicationAllocation allocation = allocations.get(0);
                    ApplicationAllocation protocolAllocation = node.retrieveAllocations().all().execute().stream().filter(allocation1 -> (Integer.parseInt(allocation1.getPort()) == Integer.parseInt(allocation.getPort()) + 100) && !allocation1.getIP().equalsIgnoreCase("127.0.0.1")).collect(Collectors.toList()).get(0);
                    ApplicationAllocation altAllocation = node.retrieveAllocations().all().execute().stream().filter(allocation1 -> Integer.parseInt(allocation1.getPort()) == Integer.parseInt(allocation.getPort()) && allocation1.getIP().equalsIgnoreCase("127.0.0.1")).collect(Collectors.toList()).get(0);
                    ApplicationAllocation altProtocolAllocation = node.retrieveAllocations().all().execute().stream().filter(allocation1 -> Integer.parseInt(allocation1.getPort()) == Integer.parseInt(allocation.getPort()) + 100 && allocation1.getIP().equalsIgnoreCase("127.0.0.1")).collect(Collectors.toList()).get(0);
                    String authKey = RandomStringUtils.randomAscii(36);
                    serverInfo = new ServerInfo(serverName, allocation.getIP(), allocation.getPortInt(), network, forced, serverType, allocation.getPortInt() + 100, coreBuild, 0, 0, 0, 0, 0, pathfinderBuild, authKey);
                    MissionControl.getDbManager().createServer(serverInfo);
                    MissionControl.getPanelManager().createPathfinderServer(serverInfo, MemoryAllocation.valueOf(serverType.getString("memory_allocation")), allocation, protocolAllocation, altAllocation, altProtocolAllocation);
                    MissionControl.getServers().get(network).put(serverName, serverInfo);
                    update = true;
                }
            }
        }

        if (update) {
            //Node was found, update the node list.
            nodes = MissionControl.getPanelManager().getAllNodes();
            try {
                if (block) {
                    waitForServerResponse(network);
                } else {
                    new Thread(() -> {
                        try {
                            waitForServerResponse(network);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }).start();
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

    public static ServerInfo createServer(String serverName, ServerType serverType, boolean forced, ServerInfo.Network network, boolean block) {
        boolean update = false;

        ServerInfo serverInfo = null;

        int coreBuild = ((network == ALPHA)?alphaBuilds.get(Module.CORE):currentCoreBuildNumber);
        int lobbyBuild = ((serverType.getModules().contains(Module.LOBBY)?((network == ALPHA)?alphaBuilds.get(Module.LOBBY):currentLobbyBuildNumber):0));
        int engineBuild = ((serverType.getModules().contains(Module.ENGINE)?((network == ALPHA)?alphaBuilds.get(Module.ENGINE):currentEngineBuildNumber):0));
        int gameBuild = ((serverType.getModules().contains(Module.GAME)?((network == ALPHA)?alphaBuilds.get(Module.GAME):currentGameBuildNumber):0));
        int buildBuild = ((serverType.getModules().contains(Module.BUILD)?((network == ALPHA)?alphaBuilds.get(Module.BUILD):currentBuildBuildNumber):0));
        int duelsBuild = ((serverType.getModules().contains(Module.DUELS)?((network == ALPHA)?alphaBuilds.get(Module.DUELS):currentDuelsBuildNumber):0));

        for (Node node : nodes) {
            if (node.getMemoryLong() - node.getAllocatedMemoryLong() > serverType.getMemoryAllocation().getMegaBytes()) {
                //There is enough memory in this node
                List<ApplicationAllocation> allocations = node.retrieveAllocations().all().execute().stream().filter(allocation -> !allocation.isAssigned() && Integer.parseInt(allocation.getPort()) < 25660 && !allocation.getIP().equalsIgnoreCase("127.0.0.1")).collect(Collectors.toList());
                if (allocations.size() > 0) {
                    ApplicationAllocation allocation = allocations.get(0);
                    ApplicationAllocation protocolAllocation = node.retrieveAllocations().all().execute().stream().filter(allocation1 -> (Integer.parseInt(allocation1.getPort()) == Integer.parseInt(allocation.getPort()) + 100) && !allocation1.getIP().equalsIgnoreCase("127.0.0.1")).collect(Collectors.toList()).get(0);
                    ApplicationAllocation altAllocation = node.retrieveAllocations().all().execute().stream().filter(allocation1 -> Integer.parseInt(allocation1.getPort()) == Integer.parseInt(allocation.getPort()) && allocation1.getIP().equalsIgnoreCase("127.0.0.1")).collect(Collectors.toList()).get(0);
                    ApplicationAllocation altProtocolAllocation = node.retrieveAllocations().all().execute().stream().filter(allocation1 -> Integer.parseInt(allocation1.getPort()) == Integer.parseInt(allocation.getPort()) + 100 && allocation1.getIP().equalsIgnoreCase("127.0.0.1")).collect(Collectors.toList()).get(0);
                    String authKey = RandomStringUtils.randomAscii(36);
                    serverInfo = new ServerInfo(serverName, allocation.getIP(), allocation.getPortInt(), network, forced, serverType.getServerTypeInformation(), allocation.getPortInt() + 100, coreBuild, lobbyBuild, engineBuild, gameBuild, buildBuild, duelsBuild, 0, authKey);
                    MissionControl.getDbManager().createServer(serverInfo);
                    MissionControl.getPanelManager().createServer(serverInfo, serverType.getMemoryAllocation(), allocation, protocolAllocation, altAllocation, altProtocolAllocation);
                    MissionControl.getServers().get(network).put(serverName, serverInfo);
                    update = true;
                }
            }
        }

        if (update) {
            //Node was found, update the node list.
            nodes = MissionControl.getPanelManager().getAllNodes();
            try {
                if (block) {
                    waitForServerResponse(network);
                } else {
                    new Thread(() -> {
                        try {
                            waitForServerResponse(network);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }).start();
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

    public static void waitForServerResponse(ServerInfo.Network network) throws InterruptedException {
        ServerInfo server = serverBlockingQueue.poll(2, TimeUnit.MINUTES);
        if (server != null) {
            if (!server.getServerType().getString("type").equalsIgnoreCase("build") && !server.getServerType().getString("type").equalsIgnoreCase("staff")) {
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
    }

    public static ServerInfo createServer(String serverName, ServerType serverType, boolean forced, ServerInfo.Network network, int coreBuild, String coreBranch, int lobbyBuild, String lobbybranch, int buildBuild, String buildBranch, int gameBuild, String gameBranch, int engineBuild, String engineBranch, int duelsBuild, String duelsBranch, boolean block) {
        boolean update = false;

        ServerInfo serverInfo = null;

        for (Node node : nodes) {
            if (node.getMemoryLong() - node.getAllocatedMemoryLong() > serverType.getMemoryAllocation().getMegaBytes()) {
                //There is enough memory in this node
                List<ApplicationAllocation> allocations = node.retrieveAllocations().all().execute().stream().filter(allocation -> !allocation.isAssigned() && Integer.parseInt(allocation.getPort()) < 25660 && !allocation.getIP().equalsIgnoreCase("127.0.0.1")).collect(Collectors.toList());
                if (allocations.size() > 0) {
                    ApplicationAllocation allocation = allocations.get(0);
                    ApplicationAllocation protocolAllocation = node.retrieveAllocations().all().execute().stream().filter(allocation1 -> (Integer.parseInt(allocation1.getPort()) == Integer.parseInt(allocation.getPort()) + 100) && !allocation1.getIP().equalsIgnoreCase("127.0.0.1")).collect(Collectors.toList()).get(0);
                    ApplicationAllocation altAllocation = node.retrieveAllocations().all().execute().stream().filter(allocation1 -> Integer.parseInt(allocation1.getPort()) == Integer.parseInt(allocation.getPort()) && allocation1.getIP().equalsIgnoreCase("127.0.0.1")).collect(Collectors.toList()).get(0);
                    ApplicationAllocation altProtocolAllocation = node.retrieveAllocations().all().execute().stream().filter(allocation1 -> Integer.parseInt(allocation1.getPort()) == Integer.parseInt(allocation.getPort()) + 100 && allocation1.getIP().equalsIgnoreCase("127.0.0.1")).collect(Collectors.toList()).get(0);
                    String authKey = RandomStringUtils.randomAscii(36);
                    serverInfo = new ServerInfo(serverName, allocation.getIP(), allocation.getPortInt(), network, forced, serverType.getServerTypeInformation(), allocation.getPortInt() + 100, coreBuild, lobbyBuild, engineBuild, gameBuild, buildBuild, duelsBuild, 0, authKey);
                    MissionControl.getDbManager().createServer(serverInfo);
                    MissionControl.getPanelManager().createServer(serverInfo, serverType.getMemoryAllocation(), allocation, protocolAllocation, altAllocation, altProtocolAllocation, coreBranch, lobbybranch, buildBranch, gameBranch, engineBranch, duelsBranch);
                    MissionControl.getServers().get(network).put(serverName, serverInfo);
                    update = true;
                }
            }
        }

        if (update) {
            //Node was found, update the node list.
            nodes = MissionControl.getPanelManager().getAllNodes();
            try {
                if (block) {
                    waitForServerResponse(network);
                } else {
                    new Thread(() -> {
                        try {
                            waitForServerResponse(network);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }).start();
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
        List<ServerInfo> infos = MissionControl.getServers().get(server.getNetwork()).values().stream().filter(info -> info.getServerType().getString("type").equalsIgnoreCase("lobby") && info.getNetwork() == server.getNetwork()).collect(Collectors.toList());
        for (ServerInfo info : infos) {
            ProtocolMessage message = new ProtocolMessage(Protocol.REMOVE_SERVER ,info.getName(), "remove", "Mission Control", server.getName());
            ServerCommunicationUtils.sendMessage(message, server.getNetwork());
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
        nodes = MissionControl.getPanelManager().getAllNodes();
    }

    public static synchronized void proxyOpenConfirmation(ProxyInfo info) {
        proxyBlockingQueue.add(info);
    }

    public static synchronized void serverOpenConfirmation(ServerInfo info) {
        serverBlockingQueue.add(info);
    }

    public static void shutdown() {
        Preferences prefs = Preferences.userNodeForPackage(MissionControl.class);
        prefs.putBoolean("clean", true);
        logger.info("Shutting down scheduler...");
        scheduler.shutdown();
        logger.info("Shutting down protocol threads...");
        ProxyCommunicationUtils.shutdown();
        ServerCommunicationUtils.shutdown();
        PanelCommunicationUtils.shutdown();
        logger.info("Shutdown complete. Goodbye!");
        System.exit(0);
    }

    public static void playerJoinedNetwork(UUID proxy) {
        MissionControl.getProxies().get(proxy).playerJoin();
    }

    public static void playerLeftNetwork(UUID proxy) {
        MissionControl.getProxies().get(proxy).playerLeave();
    }

    public static void playerJoinedServer(String server, ServerInfo.Network network) {
        MissionControl.getServers().get(network).get(server).playerJoin();
    }

    public static void playerLeftServer(String server, ServerInfo.Network network) {
        MissionControl.getServers().get(network).get(server).playerLeave();
    }

    public static void reportServerTotal(String server, int amount, ServerInfo.Network network) {
        MissionControl.getServers().get(network).get(server).setPlayerCount((byte) amount);
        if (MissionControl.getServers().get(network).get(server).getStatus() != ServerInfo.ServerStatus.PENDING_RESTART) {
            MissionControl.getServers().get(network).get(server).setStatus(ServerInfo.ServerStatus.ONLINE);
        }
    }

    public static void reportProxyTotal(UUID proxy, int amount) {
        MissionControl.getProxies().get(proxy).setPlayerCount((byte) amount);
        if (MissionControl.getProxies().get(proxy).getStatus() != ProxyInfo.ProxyStatus.PENDING_RESTART) {
            MissionControl.getProxies().get(proxy).setStatus(ProxyInfo.ProxyStatus.ONLINE);
        }

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

    public static int getCurrentDuelsBuildNumber() {
        return currentDuelsBuildNumber;
    }

    public static int getCurrentPathfinderBuildNumber() {
        return currentPathfinderBuildNumber;
    }

    public static NetworkMonitorRunnable getAlphaMonitorRunnable() {
        return alphaMonitorRunnable;
    }

    public static NetworkMonitorRunnable getMonitorRunnable() {
        return monitorRunnable;
    }

    public static NetworkRestarterThread getRestarterThread() {
        return serverRestarterThread;
    }

    public static ProxyRestarterThread getProxyRestarterThread() {
        return proxyRestarterThread;
    }

    public static boolean isAlphaEnabled() {
        return alphaEnabled;
    }

    public static boolean isServerMonitoringEnabled(ServerInfo.Network network) {
        return serverMonitorEnabled.get(network);
    }

    public static boolean isGameEnabled(ServerType serverType, ServerInfo.Network network) {
        return gameEnabled.get(network).get(serverType);
    }

    public static boolean isGameMonitored(ServerType serverType, ServerInfo.Network network) {
        return gameMonitor.get(network).get(serverType);
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
        return serverRestarterThread != null;
    }

    public static boolean isProxyUpdate() {
        return proxyRestarterThread != null;
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

    public static boolean isMaintenance(ServerInfo.Network network) {
        return NetworkManager.maintenance.get(network);
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

    public static void enableGame(ServerType serverType, ServerInfo.Network network) {
        if (network == null) {
            for (ServerInfo.Network net : ServerInfo.Network.values()) {
                gameEnabled.get(net).put(serverType, true);
                dbManager.setGameEnabled(serverType, net, true);
            }
        } else {
            gameEnabled.get(network).put(serverType, true);
            dbManager.setGameEnabled(serverType, network, true);
        }
    }

    public static void disableGame(ServerType serverType, ServerInfo.Network network) {
        if (network == null) {
            for (ServerInfo.Network net : ServerInfo.Network.values()) {
                gameEnabled.get(net).put(serverType, false);
                dbManager.setGameEnabled(serverType, net, false);
                Collection<ServerInfo> infos = MissionControl.getServers().get(net).values().stream().filter(serverInfo -> serverInfo.getServerType().getString("game").equalsIgnoreCase(serverType.name())).collect(Collectors.toList());
                for (ServerInfo info : infos) {
                    ProtocolMessage message = new ProtocolMessage(Protocol.SHUTDOWN ,info.getName(), "shutdown", "Mission Control", "");
                    ServerCommunicationUtils.sendMessage(message, info.getNetwork());
                }
            }
        } else {
            gameEnabled.get(network).put(serverType, false);
            dbManager.setGameEnabled(serverType, network, false);
        }
    }

    public static void setMonitored(ServerType serverType, ServerInfo.Network network, boolean monitored) {
        if (network == null) {
            for (ServerInfo.Network net : ServerInfo.Network.values()) {
                gameMonitor.get(net).put(serverType, monitored);
                dbManager.setMonitoringEnabled(serverType, net, monitored);
            }
        } else {
            gameMonitor.get(network).put(serverType, monitored);
            dbManager.setMonitoringEnabled(serverType, network, monitored);
        }
    }
}
