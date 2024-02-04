/*
 * Copyright (c) 2023-2024 Ethan P-B. All Rights Reserved.
 */

package net.auroramc.pathfinder.missioncontrol.backend.communication.handlers;

import net.auroramc.core.api.backend.communication.ServerCommunicationUtils;
import net.auroramc.missioncontrol.MissionControl;
import net.auroramc.missioncontrol.NetworkManager;
import net.auroramc.missioncontrol.entities.ServerInfo;
import net.auroramc.pathfinder.missioncontrol.backend.communication.PathfinderCommunicationUtils;
import net.auroramc.pathfinder.missioncontrol.backend.communication.ProtocolMessage;
import net.auroramc.proxy.api.backend.communication.Protocol;
import net.auroramc.proxy.api.backend.communication.ProxyCommunicationUtils;
import org.json.JSONObject;

import java.util.UUID;
import java.util.stream.Collectors;

public class MissionControlMessageHandler {

    public static void onMessage(ProtocolMessage message) {
        switch (message.getProtocol()) {
            case SEND: {
                ServerInfo.Network network = ServerInfo.Network.valueOf(message.getNetwork());
                ServerInfo info = MissionControl.getServers().get(network).get(message.getExtraInfo());
                UUID uuid = UUID.fromString(message.getCommand());
                UUID proxy = MissionControl.getDbManager().getProxy(uuid);
                String name = MissionControl.getDbManager().getNameFromUUID(uuid.toString());
                ProxyCommunicationUtils.sendMessage(new net.auroramc.proxy.api.backend.communication.ProtocolMessage(Protocol.SEND, proxy.toString(), name, "Mission Control", info.getName()));
            }
            case EMERGENCY_SHUTDOWN:
            case SHUTDOWN: {
                String name = message.getCommand();
                ServerInfo.Network network = ServerInfo.Network.valueOf(message.getExtraInfo());
                ServerInfo info = MissionControl.getServers().get(network).get(name);

                net.auroramc.core.api.backend.communication.ProtocolMessage msg = new net.auroramc.core.api.backend.communication.ProtocolMessage(((message.getProtocol() == net.auroramc.pathfinder.missioncontrol.backend.communication.Protocol.EMERGENCY_SHUTDOWN)?net.auroramc.core.api.backend.communication.Protocol.EMERGENCY_SHUTDOWN:net.auroramc.core.api.backend.communication.Protocol.SHUTDOWN), info.getName(), "close", "Mission Control", "");
                ServerCommunicationUtils.sendMessage(msg, network);
            }
            case CREATE_SERVER: {
                String name = message.getCommand();
                String[] args = message.getExtraInfo().split(";");
                JSONObject type = new JSONObject(args[0]);
                boolean forced = Boolean.parseBoolean(args[1]);
                ServerInfo.Network network = ServerInfo.Network.valueOf(args[2]);

                NetworkManager.createPathfinderServer(name, type, forced, network, true);
            }
        }
    }

}
