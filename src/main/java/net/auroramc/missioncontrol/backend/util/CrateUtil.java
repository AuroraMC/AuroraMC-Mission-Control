/*
 * Copyright (c) 2022-2024 Ethan P-B. All Rights Reserved.
 */

package net.auroramc.missioncontrol.backend.util;

import net.auroramc.missioncontrol.MissionControl;

import java.util.UUID;

public class CrateUtil {

    public static final long IRON_CRATE_PRICE = 2000;
    public static final long GOLD_CRATE_PRICE = 10000;
    public static final long DIAMOND_CRATE_PRICE = 20000;


    public static UUID generateIronCrate(int owner) {
        UUID uuid = UUID.randomUUID();
        MissionControl.getDbManager().newCrate(uuid, "IRON", owner);
        return uuid;
    }

    public static UUID generateGoldCrate(int owner) {
        UUID uuid = UUID.randomUUID();
        MissionControl.getDbManager().newCrate(uuid, "GOLD", owner);
        return uuid;
    }

    public static UUID generateDiamondCrate(int owner) {
        UUID uuid = UUID.randomUUID();
        MissionControl.getDbManager().newCrate(uuid, "DIAMOND", owner);
        return uuid;
    }

}
