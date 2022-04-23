/*
 * Copyright (c) 2021 AuroraMC Ltd. All Rights Reserved.
 */

package net.auroramc.missioncontrol.entities;

import org.json.JSONObject;

public class ServerInfo implements Info {

    private final String name, ip;
    private final JSONObject serverType;
    private final int protocolPort, port;
    private int buildNumber, lobbyBuildNumber, gameBuildNumber, engineBuildNumber, buildBuildNumber, duelsBuildNumber;
    private final Network network;
    private final boolean forced;
    private final String authKey;
    //Any playercount value of -1 means the value has yet to be sent back to mission control.
    private byte playerCount;
    private ServerStatus status;

    public ServerInfo(String name, String ip, int port, Network network, boolean forced, JSONObject serverType, int protocolPort, int buildNumber, int lobbyBuildNumber, int engineBuildNumber, int gameBuildNumber, int buildBuildNumber, int duelsBuildNumber, String authKey) {
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
        this.authKey = authKey;
        this.playerCount = -1;
        this.status = ServerStatus.STARTING;
        this.duelsBuildNumber = duelsBuildNumber;
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

    public int getDuelsBuildNumber() {
        return duelsBuildNumber;
    }

    public void setDuelsBuildNumber(int duelsBuildNumber) {
        this.duelsBuildNumber = duelsBuildNumber;
    }

    public String getAuthKey() {
        return authKey;
    }

    public synchronized byte getPlayerCount() {
        return playerCount;
    }

    public synchronized void setPlayerCount(byte playerCount) {
        this.playerCount = playerCount;
    }

    public synchronized void playerJoin() {
        playerCount++;
    }

    public synchronized void playerLeave() {
        playerCount--;
    }


    public ServerStatus getStatus() {
        return status;
    }

    public void setStatus(ServerStatus status) {
        this.status = status;
    }

    public enum Network {MAIN, TEST, ALPHA}
    public enum ServerStatus {STARTING, ONLINE, PENDING_RESTART, RESTARTING}
}
