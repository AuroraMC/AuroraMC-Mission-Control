/*
 * Copyright (c) 2021-2024 Ethan P-B. All Rights Reserved.
 */

package net.auroramc.missioncontrol.backend.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public enum MaintenanceMode {

    STAFF_ONLY("Staff Only"),
    LEADERSHIP_ONLY("Leadership Only"),
    LOCKDOWN("Essential Staff Only"),
    NOT_OPEN("Not Open");

    private final String title;

    MaintenanceMode(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }
}

