/*
 * Copyright (c) 2021-2024 Ethan P-B. All Rights Reserved.
 */

package net.auroramc.missioncontrol.backend.util;

import net.auroramc.missioncontrol.MissionControl;
import net.auroramc.missioncontrol.backend.runnables.StatUpdateRunnable;
import net.auroramc.missioncontrol.entities.ProxyInfo;
import net.auroramc.missioncontrol.entities.ServerInfo;

import java.util.ArrayList;

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

    public int getStat(Game game, StatUpdateRunnable.StatisticPeriod updateFrequency, ServerType type) {
        switch (this) {
            case NETWORK_PLAYER_TOTALS: {
                int amount = 0;
                for (ProxyInfo info : MissionControl.getProxies().values()) {
                    if (info.getNetwork() == ServerInfo.Network.MAIN) {
                        amount += info.getPlayerCount();
                    }
                }
                return amount;
            }
            case NETWORK_PROXY_TOTALS:
                return (int) MissionControl.getProxies().values().stream().filter(proxyInfo -> proxyInfo.getNetwork() == ServerInfo.Network.MAIN).count();
            case GAMES_STARTED:
                return MissionControl.getDbManager().getGamesStarted(game, updateFrequency);
            case PLAYERS_PER_GAME:
                return MissionControl.getDbManager().getPlayersPerGame(game, updateFrequency);
            case GAME_PLAYER_TOTAL: {
                int amount = 0;
                for (ServerInfo info : MissionControl.getServers().get(ServerInfo.Network.MAIN).values()) {
                    if (info.getServerType().getString("game").equals(game.name()) || (info.getServerType().has("rotation") && info.getServerType().getJSONArray("rotation").toList().contains(game.name()))) {
                        amount += info.getPlayerCount();
                    }
                }
                return amount;
            }
            case UNIQUE_PLAYER_JOINS:
                return MissionControl.getDbManager().getUniquePlayerJoins(updateFrequency);
            case UNIQUE_PLAYER_TOTALS:
                return MissionControl.getDbManager().getUniquePlayerTotals();
            case NETWORK_SERVER_TOTALS:
                return (int) MissionControl.getServers().get(ServerInfo.Network.MAIN).values().stream().filter(serverInfo -> serverInfo.getServerType().getString("game").equalsIgnoreCase(type.name())).count();
            default:
                return 0;
        }
    }

}
