/*
 * Copyright (c) 2021-2023 AuroraMC Ltd. All Rights Reserved.
 *
 * PRIVATE AND CONFIDENTIAL - Distribution and usage outside the scope of your job description is explicitly forbidden except in circumstances where a company director has expressly given written permission to do so.
 */

package net.auroramc.pathfinder.missioncontrol.backend.communication;

import java.io.Serializable;
import java.util.UUID;

public class ProtocolMessage implements Serializable {

    private String authenticationKey;
    private final Protocol protocol;
    private final String command;
    private String server;
    private String network;
    private final String extraInfo;
    private final UUID uuid;

    public ProtocolMessage(String authenticationKey, Protocol protocol, String command, String server, String network, String extraInfo) {
        this.authenticationKey = authenticationKey;
        this.protocol = protocol;
        this.extraInfo = extraInfo;
        this.server = server;
        this.network = network;
        this.command = command;
        this.uuid = UUID.randomUUID();
    }

    public ProtocolMessage(Protocol protocol, String command, String extraInfo) {
        this.protocol = protocol;
        this.extraInfo = extraInfo;
        this.command = command;
        this.uuid = UUID.randomUUID();
    }

    public Protocol getProtocol() {
        return protocol;
    }

    public String getCommand() {
        return command;
    }


    public String getExtraInfo() {
        return extraInfo;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getNetwork() {
        return network;
    }

    public String getAuthenticationKey() {
        return authenticationKey;
    }

    public void setNetwork(String network) {
        this.network = network;
    }

    public void setAuthenticationKey(String authenticationKey) {
        this.authenticationKey = authenticationKey;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public String getServer() {
        return server;
    }
}
