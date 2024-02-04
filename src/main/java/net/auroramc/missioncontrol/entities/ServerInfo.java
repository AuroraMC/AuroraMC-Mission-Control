/*
 * Copyright (c) 2021-2024 Ethan P-B. All Rights Reserved.
 */

package net.auroramc.missioncontrol.entities;

import org.json.JSONObject;

public class ServerInfo implements Info {

    private final String name, ip;
    private final JSONObject serverType;
    private final int protocolPort, port;
    private int buildNumber, lobbyBuildNumber, gameBuildNumber, engineBuildNumber, buildBuildNumber, duelsBuildNumber, pathfinderBuildNumber;
    private String coreBranch, lobbyBranch, gameBranch, engineBranch, buildBranch, duelsBranch, pathfinderBranch;
    private final Network network;
    private final boolean forced;
    private final String authKey;
    //Any playercount value of -1 means the value has yet to be sent back to mission control.
    private byte playerCount;
    private ServerStatus status;
    private long lastPing;

    public ServerInfo(String name, String ip, int port, Network network, boolean forced, JSONObject serverType, int protocolPort, int buildNumber, int lobbyBuildNumber, int engineBuildNumber, int gameBuildNumber, int buildBuildNumber, int duelsBuildNumber, int pathfinderBuildNumber, String authKey, String coreBranch, String lobbyBranch, String gameBranch, String engineBranch, String buildBranch, String duelsBranch, String pathfinderBranch) {
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
        this.pathfinderBuildNumber = pathfinderBuildNumber;
        this.duelsBuildNumber = duelsBuildNumber;

        this.coreBranch = coreBranch;
        this.lobbyBranch = lobbyBranch;
        this.engineBranch = engineBranch;
        this.gameBranch = gameBranch;
        this.duelsBranch = duelsBranch;
        this.pathfinderBranch = pathfinderBranch;
        this.buildBranch = buildBranch;


        this.network = network;
        this.forced = forced;
        this.authKey = authKey;
        this.playerCount = -1;
        this.status = ServerStatus.STARTING;
        this.lastPing = System.currentTimeMillis();
    }

    public String getBuildBranch() {
        return buildBranch;
    }

    public String getCoreBranch() {
        return coreBranch;
    }

    public String getDuelsBranch() {
        return duelsBranch;
    }

    public String getEngineBranch() {
        return engineBranch;
    }

    public String getGameBranch() {
        return gameBranch;
    }

    public String getLobbyBranch() {
        return lobbyBranch;
    }

    public String getPathfinderBranch() {
        return pathfinderBranch;
    }

    public void setBuildBranch(String buildBranch) {
        this.buildBranch = buildBranch;
    }

    public void setCoreBranch(String coreBranch) {
        this.coreBranch = coreBranch;
    }

    public void setDuelsBranch(String duelsBranch) {
        this.duelsBranch = duelsBranch;
    }

    public void setEngineBranch(String engineBranch) {
        this.engineBranch = engineBranch;
    }

    public void setGameBranch(String gameBranch) {
        this.gameBranch = gameBranch;
    }

    public void setPathfinderBranch(String pathfinderBranch) {
        this.pathfinderBranch = pathfinderBranch;
    }

    public void setLobbyBranch(String lobbyBranch) {
        this.lobbyBranch = lobbyBranch;
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

    public int getPathfinderBuildNumber() {
        return pathfinderBuildNumber;
    }

    public void setPathfinderBuildNumber(int pathfinderBuildNumber) {
        this.pathfinderBuildNumber = pathfinderBuildNumber;
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

    public void ping() {
        this.lastPing = System.currentTimeMillis();
    }

    public long getLastPing() {
        return lastPing;
    }
}
