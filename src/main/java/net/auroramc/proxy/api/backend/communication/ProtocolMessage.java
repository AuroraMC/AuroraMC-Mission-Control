/*
 * Copyright (c) 2021-2023 AuroraMC Ltd. All Rights Reserved.
 *
 * PRIVATE AND CONFIDENTIAL - Distribution and usage outside the scope of your job description is explicitly forbidden except in circumstances where a company director has expressly given written permission to do so.
 */

package net.auroramc.proxy.api.backend.communication;


import java.io.Serializable;
import java.util.UUID;

public class ProtocolMessage implements Serializable {

    private String authenticationKey;
    private final Protocol protocol;
    private final String destination;
    private final String command;
    private final String sender;
    private UUID proxy;
    private String network;
    private final String extraInfo;
    private final UUID uuid;

    public ProtocolMessage(Protocol protocol, String destination, String command, String sender, String extraInfo) {
        this.protocol = protocol;
        this.destination = destination;
        this.extraInfo = extraInfo;
        this.sender = sender;
        this.command = command;
        this.uuid = UUID.randomUUID();
    }

    public ProtocolMessage(String authenticationKey, Protocol protocol, String destination, String command, String sender, UUID proxy, String network, String extraInfo) {
        this.authenticationKey = authenticationKey;
        this.protocol = protocol;
        this.destination = destination;
        this.extraInfo = extraInfo;
        this.sender = sender;
        this.proxy = proxy;
        this.network = network;
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

    public UUID getProxy() {
        return proxy;
    }

    public void setProxy(UUID proxy) {
        this.proxy = proxy;
    }

    public String getNetwork() {
        return network;
    }

    public void setNetwork(String network) {
        this.network = network;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getAuthenticationKey() {
        return authenticationKey;
    }

    public void setAuthenticationKey(String authenticationKey) {
        this.authenticationKey = authenticationKey;
    }
}
