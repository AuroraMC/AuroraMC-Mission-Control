/*
 * Copyright (c) 2021 AuroraMC Ltd. All Rights Reserved.
 */

package net.auroramc.missioncontrol.backend.runnables;

import net.auroramc.missioncontrol.MissionControl;
import net.auroramc.missioncontrol.backend.util.Game;
import net.auroramc.missioncontrol.backend.util.ServerType;
import net.auroramc.missioncontrol.backend.util.Statistic;

public class StatUpdateRunnable implements Runnable {

    private final StatisticPeriod frequency;

    public StatUpdateRunnable(StatisticPeriod frequency) {
        this.frequency = frequency;
    }

    @Override
    public void run() {
        long timestamp = System.currentTimeMillis();
        for (Statistic statistic : Statistic.values()) {
            if (statistic.isSplitIntoGame()) {
                for (Game game : Game.values()) {
                    MissionControl.getDbManager().insertStatistic(statistic, frequency, timestamp, statistic.getStat(game, frequency), game);
                }
            } else {
                MissionControl.getDbManager().insertStatistic(statistic, frequency, timestamp, statistic.getStat(null, frequency));
            }
        }
    }

    public enum StatisticPeriod {DAILY, WEEKLY, ALLTIME}
}
