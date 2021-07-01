package net.auroramc.missioncontrol.entities;

import java.util.UUID;

public class ProxyInfo {

    private final UUID uuid;
    private final String ip;
    private final int port;
    private final int protocolPort;
    private final int buildNumber;

    public ProxyInfo(UUID uuid, String ip, int port, int protocolPort, int buildNumber) {
        this.uuid = uuid;
        this.ip = ip;
        this.port = port;
        this.protocolPort = protocolPort;
        this.buildNumber = buildNumber;
    }

    public UUID getUuid() {
        return uuid;
    }

    public int getPort() {
        return port;
    }

    public String getIp() {
        return ip;
    }

    public int getProtocolPort() {
        return protocolPort;
    }

    public int getBuildNumber() {
        return buildNumber;
    }
}
