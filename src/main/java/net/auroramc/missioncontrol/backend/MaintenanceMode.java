/*
 * Copyright (c) 2021 AuroraMC Ltd. All Rights Reserved.
 */

package net.auroramc.missioncontrol.backend;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public enum MaintenanceMode {

    STAFF_ONLY("Staff Only"),
    LEADERSHIP_ONLY("Leadership Only"),
    LOCKDOWN("Essential Staff Only");

    private final String title;

    MaintenanceMode(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }
}

