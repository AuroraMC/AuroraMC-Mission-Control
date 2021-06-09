package net.auroramc.missioncontrol.entities;

import org.json.JSONObject;

public class ServerInfo {

    private final String name;
    private final String ip;
    private final int port;
    private final JSONObject serverType;
    private final int protocolPort;

    public ServerInfo(String name, String ip, int port, JSONObject serverType, int protocolPort) {
        this.name = name;
        this.ip = ip;
        this.port = port;
        this.serverType = serverType;
        this.protocolPort = protocolPort;
    }

    public String getName() {
        return name;
    }

    public int getPort() {
        return port;
    }

    public JSONObject getServerType() {
        return serverType;
    }

    public String getIp() {
        return ip;
    }

    public int getProtocolPort() {
        return protocolPort;
    }

}
