/*
 * Copyright (c) 2021 AuroraMC Ltd. All Rights Reserved.
 */

package net.auroramc.missioncontrol.backend;

import net.auroramc.missioncontrol.MissionControl;
import net.auroramc.missioncontrol.NetworkManager;
import net.auroramc.missioncontrol.entities.ServerInfo;

public class PlayerCountUpdateRunnable implements Runnable{

    @Override
    public void run() {
        for (ServerInfo.Network network : ServerInfo.Network.values()) {
            MissionControl.getDbManager().pushPlayerCount(network, NetworkManager.getNetworkPlayerTotal().get(network));
        }
    }

}
