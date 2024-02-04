/*
 * Copyright (c) 2021-2024 Ethan P-B. All Rights Reserved.
 */

package net.auroramc.core.api.backend.communication;

import java.io.Serializable;
import java.util.UUID;

public class ProtocolMessage implements Serializable {

    private String authenticationKey;
    private final Protocol protocol;
    private final String destination;
    private final String command;
    private final String sender;
    private String server;
    private String network;
    private final String extraInfo;
    private final UUID uuid;

    public ProtocolMessage(String authenticationKey, Protocol protocol, String destination, String command, String sender, String server, String network, String extraInfo) {
        this.authenticationKey = authenticationKey;
        this.protocol = protocol;
        this.destination = destination;
        this.extraInfo = extraInfo;
        this.sender = sender;
        this.server = server;
        this.network = network;
        this.command = command;
        this.uuid = UUID.randomUUID();
    }

    public ProtocolMessage(Protocol protocol, String destination, String command, String sender, String extraInfo) {
        this.protocol = protocol;
        this.destination = destination;
        this.extraInfo = extraInfo;
        this.sender = sender;
        this.command = command;
        this.uuid = UUID.randomUUID();
    }

    public Protocol getProtocol() {
        return protocol;
    }

    public String getCommand() {
        return command;
    }

    public String getDestination() {
        return destination;
    }

    public String getExtraInfo() {
        return extraInfo;
    }

    public String getSender() {
        return sender;
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
