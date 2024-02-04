/*
 * Copyright (c) 2021-2024 Ethan P-B. All Rights Reserved.
 */

package net.auroramc.missioncontrol.backend.runnables;

import net.auroramc.core.api.backend.communication.Protocol;
import net.auroramc.core.api.backend.communication.ProtocolMessage;
import net.auroramc.core.api.backend.communication.ServerCommunicationUtils;
import net.auroramc.missioncontrol.MissionControl;
import net.auroramc.missioncontrol.entities.ProxyInfo;
import net.auroramc.missioncontrol.entities.ServerInfo;
import net.auroramc.proxy.api.backend.communication.ProxyCommunicationUtils;

public class RequestPlayerCountUpdateRunnable implements Runnable {

    @Override
    public void run() {
        MissionControl.getLogger().fine("Requesting updated player counts for all servers.");
        for (ServerInfo.Network network : ServerInfo.Network.values()) {
            for (ServerInfo info : MissionControl.getServers().get(network).values()) {
                ProtocolMessage message = new ProtocolMessage(Protocol.UPDATE_PLAYER_COUNT, info.getName(), "update", "MissionControl", "");
                ServerCommunicationUtils.sendMessage(message, network);
            }
        }
        for (ProxyInfo info : MissionControl.getProxies().values()) {
            net.auroramc.proxy.api.backend.communication.ProtocolMessage message = new net.auroramc.proxy.api.backend.communication.ProtocolMessage(net.auroramc.proxy.api.backend.communication.Protocol.UPDATE_PLAYER_COUNT, info.getUuid().toString(), "update", "MissionControl", "");
            ProxyCommunicationUtils.sendMessage(message);
        }
    }
}
