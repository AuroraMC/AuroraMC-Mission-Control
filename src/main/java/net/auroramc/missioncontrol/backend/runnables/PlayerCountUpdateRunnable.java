/*
 * Copyright (c) 2021-2023 AuroraMC Ltd. All Rights Reserved.
 *
 * PRIVATE AND CONFIDENTIAL - Distribution and usage outside the scope of your job description is explicitly forbidden except in circumstances where a company director has expressly given written permission to do so.
 */

package net.auroramc.missioncontrol.backend.runnables;

import net.auroramc.missioncontrol.MissionControl;
import net.auroramc.missioncontrol.entities.ProxyInfo;
import net.auroramc.missioncontrol.entities.ServerInfo;

import java.util.HashMap;
import java.util.Map;

public class PlayerCountUpdateRunnable implements Runnable {

    @Override
    public void run() {
        MissionControl.getLogger().fine("Updating Player Counts");
        Map<ServerInfo.Network, Integer> counts = new HashMap<>();
        for (ProxyInfo info : MissionControl.getProxies().values()) {
            if (!counts.containsKey(info.getNetwork())) {
                counts.put(info.getNetwork(), (int) info.getPlayerCount());
            } else {
                counts.computeIfPresent(info.getNetwork(), (key, value) -> value + info.getPlayerCount());
            }
        }
        for (Map.Entry<ServerInfo.Network, Integer> entry : counts.entrySet()) {
            MissionControl.getDbManager().pushPlayerCount(entry.getKey(), entry.getValue());
        }
    }

}
