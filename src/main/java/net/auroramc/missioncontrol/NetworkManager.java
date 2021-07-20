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
    private static final ArrayBlockingQueue<ProxyInfo> serverBlockingQueue = new ArrayBlockingQueue<>(20);

    /*
     * Build numbers for each module.
     */
    private static int currentCoreBuildNumber;
    private static int currentLobbyBuildNumber;
    private static int currentBuildBuildNumber;
    private static int currentProxyBuildNumber;
    private static int currentEngineBuildNumber;
    private static int currentGameBuildNumber;

    private static int networkPlayerTotal;
    private static final Map<Game, Integer> gamePlayerTotals;
    private static final Map<String, Integer> serverPlayerTotals;
    private static final Map<UUID, Integer> nodePlayerTotals;
    private static final Logger logger;
    private static final DatabaseManager dbManager;
    private static final ScheduledExecutorService scheduler;

    private static Map<ServerInfo.Network, String> motd;
    private static Map<ServerInfo.Network, Boolean> maintenance;
    private static Map<ServerInfo.Network, String> maintenanceMode;
    private static Map<ServerInfo.Network, String> maintenanceMotd;

    private static List<Node> nodes;
    private static NetworkRestarterThread restarterThread;


    static {
        networkPlayerTotal = 0;
        gamePlayerTotals = new HashMap<>();
        serverPlayerTotals = new HashMap<>();
        nodePlayerTotals = new HashMap<>();
        logger = MissionControl.getLogger();
        dbManager = MissionControl.getDbManager();

        nodes = MissionControl.getPanelManager().getAllNodes();

        scheduler = Executors.newSingleThreadScheduledExecutor();
        shutdown = false;

        for (Game game : Game.values()) {
            gamePlayerTotals.put(game, 0);
        }
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
        for (ServerInfo info : MissionControl.getServers().values()) {
            ProtocolMessage message = new ProtocolMessage(Protocol.UPDATE_PLAYER_COUNT, info.getName(), "update", "MissionControl", "");
            ServerCommunicationUtils.sendMessage(message);
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

        NetworkMonitorRunnable runnable = new NetworkMonitorRunnable(logger);
        scheduler.scheduleWithFixedDelay(runnable, 1, 1, TimeUnit.MINUTES);
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
            net.auroramc.proxy.api.backend.communication.ProtocolMessage message = new net.auroramc.proxy.api.backend.communication.ProtocolMessage(net.auroramc.proxy.api.backend.communication.Protocol.UPDATE_MOTD, uuid.toString(), "update", "Mission Control", "Update in progress - Limited network capacity");
            ProxyCommunicationUtils.sendMessage(message);
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
                    ProxyInfo info = new ProxyInfo(uuid, allocation.getIP(), allocation.getPortInt(), network, forced, allocation.getPortInt() + 10, currentProxyBuildNumber);
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
        MissionControl.getProxyManager().removeServer(info.getUuid().toString());
    }

    public static void deleteProxy(ProxyInfo info) {
        MissionControl.getPanelManager().deleteServer(info.getUuid().toString());
        MissionControl.getDbManager().deleteNode(info);
        MissionControl.getProxies().remove(info.getUuid());
        nodePlayerTotals.remove(info.getUuid());
        nodes = MissionControl.getPanelManager().getAllNodes();
    }
    public static synchronized void proxyOpenConfirmation(ProxyInfo info) {
        proxyBlockingQueue.add(info);
    }

    public static void shutdown() {
        logger.info("Shutting down scheduler...");
        scheduler.shutdown();
        logger.info("Shutting down protocol threads...");
        ProxyCommunicationUtils.shutdown();
        ServerCommunicationUtils.shutdown();
        logger.info("Shutting down protocol threads...");
    }

    public static void playerJoinedNetwork(UUID proxy) {
        synchronized (lock) {
            networkPlayerTotal++;
            if (nodePlayerTotals.containsKey(proxy)) {
                nodePlayerTotals.put(proxy, nodePlayerTotals.get(proxy) + 1);
            } else {
                nodePlayerTotals.put(proxy, 1);
            }
        }
    }

    public static void playerLeftNetwork(UUID proxy) {
        synchronized (lock) {
            networkPlayerTotal--;
            if (nodePlayerTotals.containsKey(proxy)) {
                int newTotal = nodePlayerTotals.get(proxy) - 1;
                if (newTotal <= 0) {
                    nodePlayerTotals.remove(proxy);
                    return;
                }
                nodePlayerTotals.put(proxy, newTotal);
            }
        }
    }

    public static void playerJoinedServer(String newServer, Game game) {
        synchronized (lock) {
            if (serverPlayerTotals.containsKey(newServer)) {
                serverPlayerTotals.put(newServer, serverPlayerTotals.get(newServer) + 1);
            } else {
                serverPlayerTotals.put(newServer, 1);
            }
            if (game != null) {
                gamePlayerTotals.put(game, gamePlayerTotals.get(game) + 1);
            }
        }
    }

    public static void playerLeftServer(String oldServer, Game game) {
        synchronized (lock) {
            if (serverPlayerTotals.containsKey(oldServer)) {
                int newTotal = serverPlayerTotals.get(oldServer) - 1;
                if (newTotal <= 0) {
                    serverPlayerTotals.remove(oldServer);
                    return;
                }
                serverPlayerTotals.put(oldServer, newTotal);
            }
            if (game != null) {
                gamePlayerTotals.put(game, gamePlayerTotals.get(game) - 1);
            }
        }
    }

    public static void reportServerTotal(String server, Game game, int amount) {
        synchronized (lock) {
            if (serverPlayerTotals.containsKey(server)) {
                gamePlayerTotals.put(game, gamePlayerTotals.get(game) - serverPlayerTotals.get(server));
                networkPlayerTotal -= serverPlayerTotals.get(server);
            }
            serverPlayerTotals.put(server, amount);
            networkPlayerTotal += amount;
            if (game != null) {
                gamePlayerTotals.put(game, gamePlayerTotals.get(game) + amount);
            }
        }
    }

    public static int getNetworkPlayerTotal() {
        return networkPlayerTotal;
    }

    public static Map<Game, Integer> getGamePlayerTotals() {
        return gamePlayerTotals;
    }

    public static Map<String, Integer> getServerPlayerTotals() {
        return serverPlayerTotals;
    }

    public static Map<UUID, Integer> getNodePlayerTotals() {
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
}
