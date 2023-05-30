/*
 * Copyright (c) 2021-2023 AuroraMC Ltd. All Rights Reserved.
 *
 * PRIVATE AND CONFIDENTIAL - Distribution and usage outside the scope of your job description is explicitly forbidden except in circumstances where a company director has expressly given written permission to do so.
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

