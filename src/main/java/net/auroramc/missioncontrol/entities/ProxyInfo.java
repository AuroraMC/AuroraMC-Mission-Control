package net.auroramc.missioncontrol.entities;

import java.util.UUID;

public class ProxyInfo {

    private final UUID uuid;
    private final String ip;
    private final int port;
    private final int protocolPort;


    public ProxyInfo(UUID uuid, String ip, int port, int protocolPort) {
        this.uuid = uuid;
        this.ip = ip;
        this.port = port;
        this.protocolPort = protocolPort;
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
}
