/*
 * Copyright (c) 2022-2024 Ethan P-B. All Rights Reserved.
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
