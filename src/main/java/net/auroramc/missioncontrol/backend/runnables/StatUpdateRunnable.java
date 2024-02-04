/*
 * Copyright (c) 2021-2024 Ethan P-B. All Rights Reserved.
 */

package net.auroramc.missioncontrol.backend.runnables;

import net.auroramc.missioncontrol.MissionControl;
import net.auroramc.missioncontrol.backend.util.Game;
import net.auroramc.missioncontrol.backend.util.ServerType;
import net.auroramc.missioncontrol.backend.util.Statistic;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class StatUpdateRunnable implements Runnable {

    private final StatisticPeriod frequency;

    public StatUpdateRunnable(StatisticPeriod frequency) {
        this.frequency = frequency;
    }

    @Override
    public void run() {

        long timestamp = System.currentTimeMillis();
        Map<Game, Integer> totals = new HashMap<>();
        Map<Game, Integer> avgPlayerPerGame = new HashMap<>();
        for (Statistic statistic : Statistic.values()) {

            if (frequency != StatisticPeriod.ALLTIME) {
                Set<String> stats = MissionControl.getDbManager().getStat(statistic, frequency);
                for (String stat : stats) {
                    String[] args = stat.split(";");
                    long time = Long.parseLong(args[0]);
                    if ((frequency == StatisticPeriod.DAILY && timestamp - time >= 86400000) || (frequency == StatisticPeriod.WEEKLY && timestamp - time >= 604800000)) {
                        MissionControl.getDbManager().removeStat(statistic, frequency, stat);
                    }
                }
            }

            if (statistic.isSplitIntoGame()) {
                if (statistic == Statistic.NETWORK_SERVER_TOTALS) {
                    for (ServerType type : ServerType.values()) {
                        int stat = statistic.getStat(null, frequency, type);
                        MissionControl.getDbManager().insertStatistic(statistic, frequency, timestamp, stat, type);
                    }
                    return;
                }
                for (Game game : Game.values()) {
                    int stat = statistic.getStat(game, frequency, null);
                    if (statistic == Statistic.GAMES_STARTED) {
                        totals.put(game, stat);
                    } if (statistic == Statistic.PLAYERS_PER_GAME) {
                        if (totals.get(game) == 0) {
                            avgPlayerPerGame.put(game, 0);
                            continue;
                        }
                        avgPlayerPerGame.put(game, stat / totals.get(game));
                        continue;
                    }
                    MissionControl.getDbManager().insertStatistic(statistic, frequency, timestamp, stat, game);
                }
                if (statistic == Statistic.PLAYERS_PER_GAME) {
                    for (Map.Entry<Game, Integer> entry : avgPlayerPerGame.entrySet()) {
                        MissionControl.getDbManager().insertStatistic(statistic, frequency, timestamp, entry.getValue(), entry.getKey());
                    }
                }
            } else {
                MissionControl.getDbManager().insertStatistic(statistic, frequency, timestamp, statistic.getStat(null, frequency, null));
            }
        }
    }

    public enum StatisticPeriod {DAILY, WEEKLY, ALLTIME}
}
