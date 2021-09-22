package net.auroramc.proxy.api.backend.communication;


import java.io.Serializable;
import java.util.UUID;

public class ProtocolMessage implements Serializable {

    private final Protocol protocol;
    private final String destination;
    private final String command;
    private final String sender;
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
}
