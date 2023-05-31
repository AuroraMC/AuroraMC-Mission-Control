/*
 * Copyright (c) 2022-2023 AuroraMC Ltd. All Rights Reserved.
 *
 * PRIVATE AND CONFIDENTIAL - Distribution and usage outside the scope of your job description is explicitly forbidden except in circumstances where a company director has expressly given written permission to do so.
 */

package net.auroramc.missioncontrol.backend.store;

import java.util.List;
import java.util.UUID;

public class CommandResponse {

    private final int commandId;
    private final List<UUID> cratesGiven;

    public CommandResponse(int commandId, List<UUID> cratesGiven) {
        this.commandId = commandId;
        this.cratesGiven = cratesGiven;
    }

    public int getCommandId() {
        return commandId;
    }

    public List<UUID> getCratesGiven() {
        return cratesGiven;
    }
}
