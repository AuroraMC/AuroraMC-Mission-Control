/*
 * Copyright (c) 2021 AuroraMC Ltd. All Rights Reserved.
 */

package net.auroramc.missioncontrol.backend.util;

public enum MemoryAllocation {

    LOBBY(2048),
    PROXY(1024),
    EVENT(3072),
    GAME(1024),
    DUELS(1536),
    BUILD(1024);

    private final long megaBytes;

    MemoryAllocation(long megaBytes) {
        this.megaBytes = megaBytes;
    }

    public long getMegaBytes() {
        return megaBytes;
    }
}
