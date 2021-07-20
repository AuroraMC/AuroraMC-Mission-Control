package net.auroramc.missioncontrol.entities;

import java.util.UUID;

public class ProxyInfo implements Info {

    private final UUID uuid;
    private final String ip;
    private final int port;
    private final int protocolPort;
    private int buildNumber;
    private final ServerInfo.Network network;
    private final boolean forced;

    public ProxyInfo(UUID uuid, String ip, int port, ServerInfo.Network network, boolean forced, int protocolPort, int buildNumber) {
        this.uuid = uuid;
        this.ip = ip;
        this.port = port;
        this.protocolPort = protocolPort;
        this.buildNumber = buildNumber;
        this.network = network;
        this.forced = forced;
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

    public ServerInfo.Network getNetwork() {
        return network;
    }

    public void setBuildNumber(int buildNumber) {
        this.buildNumber = buildNumber;
    }

    public boolean isForced() {
        return forced;
    }
}
