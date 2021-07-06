package net.auroramc.missioncontrol;

import net.auroramc.missioncontrol.backend.Game;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class NetworkMonitorRunnable implements Runnable {

    private int networkPlayerTotal;
    private Map<Game, Integer> gamePlayerTotals;
    private Map<String, Integer> serverPlayerTotals;
    private Map<UUID, Integer> nodePlayerTotals;
    private final Logger logger;

    public NetworkMonitorRunnable(Logger logger) {
        this.logger = logger;
        networkPlayerTotal = NetworkManager.getNetworkPlayerTotal();
        gamePlayerTotals = new HashMap<>(NetworkManager.getGamePlayerTotals());
        serverPlayerTotals = new HashMap<>(NetworkManager.getServerPlayerTotals());
        nodePlayerTotals = new HashMap<>(NetworkManager.getNodePlayerTotals());
    }

    @Override
    public void run() {

    }

}
