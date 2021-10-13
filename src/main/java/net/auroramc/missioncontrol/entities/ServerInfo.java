/*
 * Copyright (c) 2021 AuroraMC Ltd. All Rights Reserved.
 */

package net.auroramc.missioncontrol.entities;

import org.json.JSONObject;

public class ServerInfo implements Info {

    private final String name, ip;
    private final JSONObject serverType;
    private final int protocolPort, port;
    private int buildNumber, lobbyBuildNumber, gameBuildNumber, engineBuildNumber, buildBuildNumber;
    private final Network network;
    private final boolean forced;

    public ServerInfo(String name, String ip, int port, Network network, boolean forced, JSONObject serverType, int protocolPort, int buildNumber, int lobbyBuildNumber, int engineBuildNumber, int gameBuildNumber, int buildBuildNumber) {
        this.name = name;
        this.ip = ip;
        this.port = port;
        this.serverType = serverType;
        this.protocolPort = protocolPort;
        this.buildNumber = buildNumber;
        this.buildBuildNumber = buildBuildNumber;
        this.engineBuildNumber = engineBuildNumber;
        this.gameBuildNumber = gameBuildNumber;
        this.lobbyBuildNumber = lobbyBuildNumber;
        this.network = network;
        this.forced = forced;
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

    public int getBuildNumber() {
        return buildNumber;
    }

    public int getBuildBuildNumber() {
        return buildBuildNumber;
    }

    public int getEngineBuildNumber() {
        return engineBuildNumber;
    }

    public int getGameBuildNumber() {
        return gameBuildNumber;
    }

    public int getLobbyBuildNumber() {
        return lobbyBuildNumber;
    }

    public boolean isForced() {
        return forced;
    }

    public Network getNetwork() {
        return network;
    }

    public void setBuildBuildNumber(int buildBuildNumber) {
        this.buildBuildNumber = buildBuildNumber;
    }

    public void setBuildNumber(int buildNumber) {
        this.buildNumber = buildNumber;
    }

    public void setEngineBuildNumber(int engineBuildNumber) {
        this.engineBuildNumber = engineBuildNumber;
    }

    public void setGameBuildNumber(int gameBuildNumber) {
        this.gameBuildNumber = gameBuildNumber;
    }

    public void setLobbyBuildNumber(int lobbyBuildNumber) {
        this.lobbyBuildNumber = lobbyBuildNumber;
    }

    public enum Network {MAIN, TEST, ALPHA}
}
