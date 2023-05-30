/*
 * Copyright (c) 2022-2023 AuroraMC Ltd. All Rights Reserved.
 *
 * PRIVATE AND CONFIDENTIAL - Distribution and usage outside the scope of your job description is explicitly forbidden except in circumstances where a company director has expressly given written permission to do so.
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
