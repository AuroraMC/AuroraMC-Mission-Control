/*
 * Copyright (c) 2022 AuroraMC Ltd. All Rights Reserved.
 */

package net.auroramc.missioncontrol.backend.store.packages.plus;

import net.auroramc.missioncontrol.MissionControl;
import net.auroramc.missioncontrol.backend.store.Package;
import net.auroramc.proxy.api.backend.communication.Protocol;
import net.auroramc.proxy.api.backend.communication.ProtocolMessage;
import net.auroramc.proxy.api.backend.communication.ProxyCommunicationUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Plus365 extends Package {


    public Plus365() {
        super(6);
    }

    @Override
    public List<UUID> onReceive(int amcId, UUID uuid) {
        long endTimestamp = MissionControl.getDbManager().getExpire(uuid);
        if (endTimestamp != -1 && endTimestamp > System.currentTimeMillis()) {
            //They already have an active subscription
            MissionControl.getDbManager().extend(uuid, 365);
                if (MissionControl.getDbManager().hasActiveSession(uuid)) {
                    ProtocolMessage message = new ProtocolMessage(Protocol.UPDATE_PROFILE, MissionControl.getDbManager().getProxy(uuid).toString(), "extend_subscription", "Mission Control", uuid + "\n" + 30);
                    ProxyCommunicationUtils.sendMessage(message);
                }
        } else {
            MissionControl.getDbManager().newSubscription(uuid, 365);
                if (MissionControl.getDbManager().hasActiveSession(uuid)) {
                    ProtocolMessage message = new ProtocolMessage(Protocol.UPDATE_PROFILE, MissionControl.getDbManager().getProxy(uuid).toString(), "new_subscription", "Mission Control", uuid.toString());
                    ProxyCommunicationUtils.sendMessage(message);
                }
        }
        return new ArrayList<>();
    }

    @Override
    public List<UUID> onChargeback(int amcId, UUID uuid) {
        //Not eligible for refunds, and chargebacks will issue a ban anyway so there's no point to this.
        return new ArrayList<>();
    }
}
