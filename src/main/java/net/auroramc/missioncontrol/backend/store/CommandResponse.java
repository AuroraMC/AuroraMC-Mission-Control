/*
 * Copyright (c) 2022 AuroraMC Ltd. All Rights Reserved.
 */

package net.auroramc.missioncontrol.backend.store;

import java.util.List;
import java.util.UUID;

public class CommandResponse {

    private final int commandId;
    private final List<UUID> cratesGiven;
    private final boolean chargeback;
    private final boolean refund;

    public CommandResponse(int commandId, List<UUID> cratesGiven, boolean chargeback, boolean refund) {
        this.commandId = commandId;
        this.cratesGiven = cratesGiven;
        this.chargeback = chargeback;
        this.refund = refund;
    }

    public int getCommandId() {
        return commandId;
    }

    public List<UUID> getCratesGiven() {
        return cratesGiven;
    }

    public boolean isChargeback() {
        return chargeback;
    }

    public boolean isRefund() {
        return refund;
    }
}
