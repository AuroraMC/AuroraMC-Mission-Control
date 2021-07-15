package net.auroramc.missioncontrol.backend;

/**
 * Server types.
 */
public enum Game {

    LOBBY(80, true),
    EVENT(100, false),
    CQ(10, true),
    MIXED_ARCADE(16, true);

    private final int maxPlayers;
    private final boolean monitor;

    Game(int maxPlayers, boolean monitor) {
        this.maxPlayers = maxPlayers;
        this.monitor = monitor;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public boolean isMonitor() {
        return monitor;
    }
}
