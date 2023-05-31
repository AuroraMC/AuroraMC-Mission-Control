/*
 * Copyright (c) 2023 AuroraMC Ltd. All Rights Reserved.
 *
 * PRIVATE AND CONFIDENTIAL - Distribution and usage outside the scope of your job description is explicitly forbidden except in circumstances where a company director has expressly given written permission to do so.
 */

package net.buycraft.plugin.platform;

import java.util.Locale;

public enum PlatformType {
    BUKKIT,
    BUNGEECORD,
    SPONGE,
    NUKKIT,
    FORGE,
    VELOCITY,
    NONE;

    public String platformName() {
        return name().toLowerCase(Locale.US);
    }
}
