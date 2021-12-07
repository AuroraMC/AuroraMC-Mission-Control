/*
 * Copyright (c) 2021 AuroraMC Ltd. All Rights Reserved.
 */

package net.auroramc.missioncontrol.backend.runnables;

import net.auroramc.missioncontrol.MissionControl;
import net.auroramc.missioncontrol.NetworkManager;
import net.auroramc.missioncontrol.entities.ProxyInfo;
import net.auroramc.missioncontrol.entities.ServerInfo;

import java.util.HashMap;
import java.util.Map;

public class PlayerCountUpdateRunnable implements Runnable{

    @Override
    public void run() {
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
