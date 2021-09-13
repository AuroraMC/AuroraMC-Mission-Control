package net.auroramc.missioncontrol;

import com.mattmalec.pterodactyl4j.application.entities.Allocation;
import com.mattmalec.pterodactyl4j.application.entities.Node;
import net.auroramc.core.api.backend.communication.Protocol;
import net.auroramc.core.api.backend.communication.ProtocolMessage;
import net.auroramc.core.api.backend.communication.ServerCommunicationUtils;
import net.auroramc.missioncontrol.backend.Game;
import net.auroramc.missioncontrol.backend.MemoryAllocation;
import net.auroramc.missioncontrol.backend.Module;
import net.auroramc.missioncontrol.backend.managers.DatabaseManager;
import net.auroramc.missioncontrol.entities.ProxyInfo;
import net.auroramc.missioncontrol.entities.ServerInfo;
import net.auroramc.proxy.api.backend.communication.ProxyCommunicationUtils;
import org.apache.log4j.Logger;

import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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
    private static Map<ServerInfo.Network, String> maintenanceMode;
    private static Map<ServerInfo.Network, String> maintenanceMotd;

    private static List<Node> nodes;
    private static NetworkRestarterThread restarterThread;
    private static NetworkMonitorRunnable monitorRunnable;
    private static NetworkMonitorRunnable alphaMonitorRunnable;

    private static boolean alphaEnabled;


    static {
        networkPlayerTotal = new HashMap<>();
        gamePlayerTotals = new HashMap<>();
        serverPlayerTotals = new HashMap<>();
        nodePlayerTotals = new HashMap<>();

        for (ServerInfo.Network network : ServerInfo.Network.values()) {
            networkPlayerTotal.put(network, 0);

            Map<Game, Integer> gameTotals = new HashMap<>();
            for (Game game : Game.values()) {
                gameTotals.put(game, 0);
            }

            gamePlayerTotals.put(network, gameTotals);
            serverPlayerTotals.put(network, new HashMap<>());
            nodePlayerTotals.put(network, new HashMap<>());
        }

        logger = MissionControl.getLogger();
        dbManager = MissionControl.getDbManager();

        nodes = MissionControl.getPanelManager().getAllNodes();

        scheduler = Executors.newScheduledThreadPool(2);
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
                    if (serverPlayerTotals.size() == MissionControl.getServers().size() && nodePlayerTotals.size() == MissionControl.getProxies().size()) {
                        logger.info("All responses received, starting network monitoring thread...");
                        break;
                    }
                    if (i == 6) {
                        logger.info("Not all responses received but timeout reached, starting network monitoring thread...");
                    } else {
                        lock2.wait(10000);
                    }
                }
            }

        } catch (InterruptedException e) {
            logger.warn("Waiting for responses was interrupted. Starting network monitoring thread... ", e);
        }

        NetworkMonitorRunnable runnable = new NetworkMonitorRunnable(logger, ServerInfo.Network.MAIN);
        scheduler.scheduleWithFixedDelay(runnable, 1, 1, TimeUnit.MINUTES);
        if (alphaEnabled) {
            runnable = new NetworkMonitorRunnable(logger, ServerInfo.Network.ALPHA);
            scheduler.scheduleWithFixedDelay(runnable, 1, 1, TimeUnit.MINUTES);
        }

        done();
    }

    private static void done() {
        logger.info("Handoff complete. Blocking main thread till shutdown.");
        try {
            synchronized (lock2) {
                while (!shutdown) {
                    lock2.wait();
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        logger.info("Interrupt received on the main thread, shutting down...");
        shutdown();
    }

    public static void interrupt() {
        shutdown = true;
        lock2.notifyAll();
    }

    public static void pushUpdate(Map<Module, Integer> module, ServerInfo.Network network) {
        //Stop the network monitor from spinning up more servers.
        NetworkMonitorRunnable.setUpdate(true);
        for (UUID uuid : MissionControl.getProxies().keySet()) {
            if (MissionControl.getProxies().get(uuid).getNetwork() == network) {
                net.auroramc.proxy.api.backend.communication.ProtocolMessage message = new net.auroramc.proxy.api.backend.communication.ProtocolMessage(net.auroramc.proxy.api.backend.communication.Protocol.UPDATE_MOTD, uuid.toString(), "update", "Mission Control", "&b&lUpdate in progress - Limited network capacity");
                ProxyCommunicationUtils.sendMessage(message);
            }
        }

        if (module.containsKey(Module.CORE)) {
            currentCoreBuildNumber = module.get(Module.CORE);
            MissionControl.getDbManager().setCurrentCoreBuildNumber(currentCoreBuildNumber);
        }
        if (module.containsKey(Module.LOBBY)) {
            currentLobbyBuildNumber = module.get(Module.LOBBY);
            MissionControl.getDbManager().setCurrentLobbyBuildNumber(currentLobbyBuildNumber);
        }
        if (module.containsKey(Module.BUILD)) {
            currentBuildBuildNumber = module.get(Module.BUILD);
            MissionControl.getDbManager().setCurrentBuildBuildNumber(currentBuildBuildNumber);
        }
        if (module.containsKey(Module.ENGINE)) {
            currentEngineBuildNumber = module.get(Module.ENGINE);
            MissionControl.getDbManager().setCurrentEngineBuildNumber(currentEngineBuildNumber);
        }
        if (module.containsKey(Module.GAME)) {
            currentGameBuildNumber = module.get(Module.GAME);
            MissionControl.getDbManager().setCurrentGameBuildNumber(currentGameBuildNumber);
        }
        if (module.containsKey(Module.PROXY)) {
            currentProxyBuildNumber = module.get(Module.PROXY);
            MissionControl.getDbManager().setCurrentProxyBuildNumber(currentProxyBuildNumber);
        }

        restarterThread = new NetworkRestarterThread(new ArrayList<>(module.keySet()), network);
        restarterThread.start();
    }

    public static void updateComplete() {
        NetworkMonitorRunnable.setUpdate(true);
        restarterThread = null;

        //Update the MOTD to what it was before.
        for (UUID uuid : MissionControl.getProxies().keySet()) {
            net.auroramc.proxy.api.backend.communication.ProtocolMessage message = new net.auroramc.proxy.api.backend.communication.ProtocolMessage(net.auroramc.proxy.api.backend.communication.Protocol.UPDATE_MOTD, uuid.toString(), "update", "Mission Control", motd.get(MissionControl.getProxies().get(uuid).getNetwork()));
            ProxyCommunicationUtils.sendMessage(message);
        }
    }

    /**
     * Create a connection node. This method blocks until the node has been confirmed to have fully started and be ready to accept connections.
     */
    public static void createProxy(ServerInfo.Network network, boolean forced) {
        UUID uuid = UUID.randomUUID();
        boolean update = false;
        for (Node node : nodes) {
            if (node.getMemoryLong() - node.getAllocatedMemoryLong() > MemoryAllocation.PROXY.getMegaBytes()) {
                //There is enough memory in this node
                List<Allocation> allocations = node.getAllocations().retrieve().execute().stream().filter(allocation -> !allocation.isAssigned()).collect(Collectors.toList());
                if (allocations.size() > 0) {
                    Allocation allocation = allocations.get(0);
                    ProxyInfo info = new ProxyInfo(uuid, allocation.getIP(), allocation.getPortInt(), network, forced, allocation.getPortInt() + 100, currentProxyBuildNumber);
                    MissionControl.getDbManager().createConnectionNode(info);
                    MissionControl.getPanelManager().createProxy(info, allocation);
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
            return;
        }

        //No nodes were found with enough memory. Ignore request.
        logger.info("A connection node was attempted to be created but a node was not found with enough memory to create it.");
    }

    public static void removeProxyFromRotation(ProxyInfo info) {
        MissionControl.getProxyManager().removeServer(info.getUuid().toString(), info.getNetwork());
    }

    public static void deleteProxy(ProxyInfo info) {
        MissionControl.getPanelManager().deleteServer(info.getUuid().toString());
        MissionControl.getDbManager().deleteNode(info);
        MissionControl.getProxies().remove(info.getUuid());
        networkPlayerTotal.put(info.getNetwork(), networkPlayerTotal.get(info.getNetwork()) - nodePlayerTotals.get(info.getNetwork()).get(info.getUuid()));
        nodePlayerTotals.get(info.getNetwork()).remove(info.getUuid());
        nodes = MissionControl.getPanelManager().getAllNodes();
    }

    public static void createServer(String serverName, Game game, boolean forced, ServerInfo.Network network) {
        boolean update = false;

        for (Node node : nodes) {
            if (node.getMemoryLong() - node.getAllocatedMemoryLong() > game.getMemoryAllocation().getMegaBytes()) {
                //There is enough memory in this node
                List<Allocation> allocations = node.getAllocations().retrieve().execute().stream().filter(allocation -> !allocation.isAssigned()).collect(Collectors.toList());
                if (allocations.size() > 0) {
                    Allocation allocation = allocations.get(0);
                    ServerInfo info = new ServerInfo(serverName, allocation.getIP(), allocation.getPortInt(), network, forced, game.getServerTypeInformation(), allocation.getPortInt() + 100, currentCoreBuildNumber, ((game.getModules().contains(Module.LOBBY)?currentLobbyBuildNumber:-1)), ((game.getModules().contains(Module.ENGINE)?currentEngineBuildNumber:-1)), ((game.getModules().contains(Module.GAME)?currentGameBuildNumber:-1)), ((game.getModules().contains(Module.BUILD)?currentBuildBuildNumber:-1)));
                    MissionControl.getDbManager().createServer(info);
                    MissionControl.getPanelManager().createServer(info, game.getMemoryAllocation(), allocation);
                    MissionControl.getServers().get(network).put(serverName, info);
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
                        List<ServerInfo> infos = MissionControl.getServers().get(network).values().stream().filter(info -> info.getServerType().getString("type").equalsIgnoreCase("lobby") && info.getNetwork() == server.getNetwork()).collect(Collectors.toList());
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
            return;
        }

        //No nodes were found with enough memory. Ignore request.
        logger.info("A server was attempted to be created but a node was not found with enough memory to create it.");
    }

    public static void removeServerFromRotation(ServerInfo server) {
        List<ServerInfo> infos = MissionControl.getServers().get(server.getNetwork()).values().stream().filter(info -> info.getServerType().getString("type").equalsIgnoreCase("lobby") && info.getNetwork() == server.getNetwork()).collect(Collectors.toList());
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
        MissionControl.getPanelManager().deleteServer(info.getName());
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
}
