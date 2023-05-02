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
    BUILD(1024),
    SMP(5120),
    GENERIC_1G(1024),
    GENERIC_1_5G(1536),
    GENERIC_2G(2048),
    GENERIC_2_5G(2560),
    GENERIC_3G(3072);

    private final long megaBytes;

    MemoryAllocation(long megaBytes) {
        this.megaBytes = megaBytes;
    }

    public long getMegaBytes() {
        return megaBytes;
    }
}
