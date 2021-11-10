/*
 * Copyright (c) 2021 AuroraMC Ltd. All Rights Reserved.
 */

package net.auroramc.missioncontrol.backend.util;

import net.auroramc.missioncontrol.MissionControl;
import net.auroramc.missioncontrol.NetworkManager;
import net.auroramc.missioncontrol.backend.runnables.StatUpdateRunnable;
import net.auroramc.missioncontrol.entities.ServerInfo;

public enum Statistic {

    NETWORK_PLAYER_TOTALS(false),
    NETWORK_PROXY_TOTALS(false),
    UNIQUE_PLAYER_TOTALS(false),
    UNIQUE_PLAYER_JOINS(false),
    NETWORK_SERVER_TOTALS(true),
    GAMES_STARTED(true),
    GAME_PLAYER_TOTAL(true),
    PLAYERS_PER_GAME(true);

    private final boolean splitIntoGame;

    Statistic(boolean splitIntoGame) {
        this.splitIntoGame = splitIntoGame;
    }

    public boolean isSplitIntoGame() {
        return splitIntoGame;
    }

    public int getStat(Game game, StatUpdateRunnable.StatisticPeriod updateFrequency) {
        switch (this) {
            case NETWORK_PLAYER_TOTALS:
                return NetworkManager.getNetworkPlayerTotal().get(ServerInfo.Network.MAIN);
            case NETWORK_PROXY_TOTALS:
                return (int) MissionControl.getProxies().values().stream().filter(proxyInfo -> proxyInfo.getNetwork() == ServerInfo.Network.MAIN).count();
            case GAMES_STARTED:
                return MissionControl.getDbManager().getGamesStarted(game, updateFrequency);
            case PLAYERS_PER_GAME:
                return MissionControl.getDbManager().getPlayersPerGame(game, updateFrequency);
            case GAME_PLAYER_TOTAL:
                return NetworkManager.getGamePlayerTotals().get(ServerInfo.Network.MAIN).get(game);
            case UNIQUE_PLAYER_JOINS:
                return MissionControl.getDbManager().getUniquePlayerJoins(updateFrequency);
            case UNIQUE_PLAYER_TOTALS:
                return MissionControl.getDbManager().getUniquePlayerTotals();
            case NETWORK_SERVER_TOTALS:
                return MissionControl.getServers().get(ServerInfo.Network.MAIN).size();
            default:
                return 0;
        }
    }

}
