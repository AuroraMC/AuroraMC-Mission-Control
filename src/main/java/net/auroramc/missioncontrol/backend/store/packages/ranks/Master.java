/*
 * Copyright (c) 2022 AuroraMC Ltd. All Rights Reserved.
 */

package net.auroramc.missioncontrol.backend.store.packages.ranks;

import net.auroramc.missioncontrol.MissionControl;
import net.auroramc.missioncontrol.backend.store.Package;
import net.auroramc.proxy.api.backend.communication.Protocol;
import net.auroramc.proxy.api.backend.communication.ProtocolMessage;
import net.auroramc.proxy.api.backend.communication.ProxyCommunicationUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Master extends Package {


    public Master() {
        super(2);
    }

    @Override
    public List<UUID> onReceive(int amcId, UUID uuid) {
        int rank = MissionControl.getDbManager().getRank(amcId);
        if (rank < 2) {
            //Anything above 5 is a set rank, so cannot be overridden. Anything above 1 is a higher rank than this one anyway.
            MissionControl.getDbManager().setRank(amcId, 2, rank);
            if (MissionControl.getDbManager().hasActiveSession(uuid)) {
                ProtocolMessage message = new ProtocolMessage(Protocol.UPDATE_PROFILE, MissionControl.getDbManager().getProxy(uuid).toString(), "set_rank", "Mission Control", 2 + "\n" + uuid.toString());
                ProxyCommunicationUtils.sendMessage(message);
            }
        }
        return new ArrayList<>();
    }

    @Override
    public List<UUID> onChargeback(int amcId, UUID uuid) {
        int rank = MissionControl.getDbManager().getRank(amcId);
        if (rank < 5) {
            //Anything above 5 is a set rank, so cannot be overridden. Anything above 1 is a higher rank than this one anyway.
            MissionControl.getDbManager().setRank(amcId, 0, rank);
            if (MissionControl.getDbManager().hasActiveSession(uuid)) {
                ProtocolMessage message = new ProtocolMessage(Protocol.UPDATE_PROFILE, MissionControl.getDbManager().getProxy(uuid).toString(), "set_rank", "Mission Control", 0 + "\n" + uuid.toString());
                ProxyCommunicationUtils.sendMessage(message);
            }
        }
        return new ArrayList<>();
    }
}
