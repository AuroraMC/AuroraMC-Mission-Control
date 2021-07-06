package net.auroramc.missioncontrol.backend;

public enum Game {

    LOBBY(80),
    EVENT(100),
    CQ(8),
    MIXED_ARCADE(16);

    private final int maxPlayers;

    Game(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }
}
