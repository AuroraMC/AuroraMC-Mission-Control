/*
 * Copyright (c) 2021-2024 Ethan P-B. All Rights Reserved.
 */

package net.auroramc.missioncontrol.backend.util;

public enum MemoryAllocation {

    LOBBY(1024),
    PROXY(1024),
    EVENT(3072),
    GAME(2048),
    DUELS(2048),
    BUILD(2048),
    SMP(8192),
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
