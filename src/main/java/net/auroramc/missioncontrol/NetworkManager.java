package net.auroramc.missioncontrol;

import net.auroramc.core.api.backend.communication.Protocol;
import net.auroramc.core.api.backend.communication.ProtocolMessage;
import net.auroramc.core.api.backend.communication.ServerCommunicationUtils;
import net.auroramc.missioncontrol.backend.Game;
import net.auroramc.missioncontrol.entities.ServerInfo;
import net.auroramc.proxy.api.backend.communication.ProxyCommunicationUtils;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class NetworkManager {

    private static final Object lock = new Object();
    private static final Object lock2 = new Object();
    private static boolean shutdown;

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
    private static final ScheduledExecutorService scheduler;

    static {
        networkPlayerTotal = 0;
        gamePlayerTotals = new HashMap<>();
        serverPlayerTotals = new HashMap<>();
        nodePlayerTotals = new HashMap<>();
        logger = MissionControl.getLogger();
        scheduler = Executors.newSingleThreadScheduledExecutor();
        shutdown = false;

        for (Game game : Game.values()) {
            gamePlayerTotals.put(game, 0);
        }
    }


    public static void handoff() {
        logger.info("Fetching pushed builds...");


        logger.info("Requesting player counts from servers...");
        for (ServerInfo info : MissionControl.getServers().values()) {
            ProtocolMessage message = new ProtocolMessage(Protocol.UPDATE_PLAYER_COUNT, info.getName(), "update", "MissionControl", "");
            ServerCommunicationUtils.sendMessage(message);
        }
        logger.info("Requests sent. Awaiting responses...");


        //Wait for a response from all of the servers (max 1 minute wait)
        try {
            for (int i = 0;i < 6;i++) {
                if (serverPlayerTotals.size() == MissionControl.getServers().size() && nodePlayerTotals.size() == MissionControl.getProxies().size()) {
                    logger.info("All responses received, starting network monitoring thread...");
                    break;
                }
                if (i == 5) {
                    logger.info("Not all responses received but timeout reached, starting network monitoring thread...");
                } else {
                    Thread.currentThread().wait(10000);
                }
            }
        } catch (InterruptedException e) {
            logger.warn("Waiting for responses was interrupted. Starting network monitoring thread... ", e);
        }

        NetworkMonitorRunnable runnable = new NetworkMonitorRunnable(logger);
        scheduler.scheduleWithFixedDelay(runnable, 0, 1, TimeUnit.MINUTES);
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
