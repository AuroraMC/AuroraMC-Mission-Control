/*
 * Copyright (c) 2021-2023 AuroraMC Ltd. All Rights Reserved.
 *
 * PRIVATE AND CONFIDENTIAL - Distribution and usage outside the scope of your job description is explicitly forbidden except in circumstances where a company director has expressly given written permission to do so.
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
