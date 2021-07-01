package net.auroramc.missioncontrol.backend;

public enum MemoryAllocation {

    LOBBY(2048),
    PROXY(1024),
    EVENT(3072),
    GAME(1024),
    BUILD(1024);

    private final long megaBytes;

    MemoryAllocation(long megaBytes) {
        this.megaBytes = megaBytes;
    }

    public long getMegaBytes() {
        return megaBytes;
    }
}
