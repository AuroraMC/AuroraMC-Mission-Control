/*
 * Copyright (c) 2022-2024 Ethan P-B. All Rights Reserved.
 */

package net.auroramc.missioncontrol.backend.store;

import java.util.List;
import java.util.UUID;

public abstract class Package {

    protected final int packageId;

    public Package(int packageId) {
        this.packageId = packageId;
    }

    public abstract List<UUID> onReceive(int amcId, UUID uuid);

    public abstract List<UUID> onChargeback(int amcId, UUID uuid);
}
