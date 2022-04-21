/*
 * Copyright (c) 2021 AuroraMC Ltd. All Rights Reserved.
 */

package net.auroramc.missioncontrol.backend.runnables;

import net.auroramc.missioncontrol.MissionControl;
import net.auroramc.missioncontrol.backend.util.Game;
import net.auroramc.missioncontrol.backend.util.ServerType;
import net.auroramc.missioncontrol.backend.util.Statistic;

import java.util.HashMap;
import java.util.Map;

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
            if (statistic.isSplitIntoGame()) {
                for (Game game : Game.values()) {
                    int stat = statistic.getStat(game, frequency);
                    if (statistic == Statistic.GAMES_STARTED) {
                        totals.put(game, stat);
                    } if (statistic == Statistic.PLAYERS_PER_GAME) {
                        avgPlayerPerGame.put(game, totals.get(game) / stat);
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
                MissionControl.getDbManager().insertStatistic(statistic, frequency, timestamp, statistic.getStat(null, frequency));
            }
        }
    }

    public enum StatisticPeriod {DAILY, WEEKLY, ALLTIME}
}
