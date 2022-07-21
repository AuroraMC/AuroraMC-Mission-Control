/*
 * Copyright (c) 2022 AuroraMC Ltd. All Rights Reserved.
 */

package net.auroramc.missioncontrol.backend.store.packages.bundles;

import net.auroramc.missioncontrol.MissionControl;
import net.auroramc.missioncontrol.backend.store.Package;
import net.auroramc.proxy.api.backend.communication.Protocol;
import net.auroramc.proxy.api.backend.communication.ProtocolMessage;
import net.auroramc.proxy.api.backend.communication.ProxyCommunicationUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Starter extends Package {


    public Starter() {
        super(8);
    }

    @Override
    public List<UUID> onReceive(int amcId, UUID uuid) {
        int rank = MissionControl.getDbManager().getRank(amcId);
        if (rank < 1) {
            //Anything above 5 is a set rank, so cannot be overridden. Anything above 1 is a higher rank than this one anyway.
            MissionControl.getDbManager().setRank(amcId, 1, rank);
            if (MissionControl.getDbManager().hasActiveSession(uuid)) {
                ProtocolMessage message = new ProtocolMessage(Protocol.UPDATE_PROFILE, MissionControl.getDbManager().getProxy(uuid).toString(), "set_rank", "Mission Control", 1 + "\n" + uuid.toString());
                ProxyCommunicationUtils.sendMessage(message);
            }
        }
        MissionControl.getDbManager().addCosmetic(uuid, 121);
        MissionControl.getDbManager().addCosmetic(uuid, 122);
        ProtocolMessage message = new ProtocolMessage(Protocol.UPDATE_PROFILE, MissionControl.getDbManager().getProxy(uuid).toString(), "add_cosmetic", "Mission Control", uuid.toString() + "\n" + 122);
        ProxyCommunicationUtils.sendMessage(message);
        message = new ProtocolMessage(Protocol.UPDATE_PROFILE, MissionControl.getDbManager().getProxy(uuid).toString(), "add_cosmetic", "Mission Control", uuid.toString() + "\n" + 121);
        ProxyCommunicationUtils.sendMessage(message);
        return new ArrayList<>();
    }

    @Override
    public List<UUID> onChargeback(int amcId, UUID uuid) {
        //Not eligible for refunds, and chargebacks will issue a ban anyway so there's no point to this.
        int rank = MissionControl.getDbManager().getRank(amcId);
        if (rank < 5) {
            //Anything above 5 is a set rank, so cannot be overridden. Anything above 1 is a higher rank than this one anyway.
            MissionControl.getDbManager().setRank(amcId, 0, rank);
            if (MissionControl.getDbManager().hasActiveSession(uuid)) {
                ProtocolMessage message = new ProtocolMessage(Protocol.UPDATE_PROFILE, MissionControl.getDbManager().getProxy(uuid).toString(), "set_rank", "Mission Control", 0 + "\n" + uuid.toString());
                ProxyCommunicationUtils.sendMessage(message);
            }
        }
        MissionControl.getDbManager().removeCosmetic(uuid, 121);
        MissionControl.getDbManager().removeCosmetic(uuid, 122);
        ProtocolMessage message = new ProtocolMessage(Protocol.UPDATE_PROFILE, MissionControl.getDbManager().getProxy(uuid).toString(), "remove_cosmetic", "Mission Control", uuid.toString() + "\n" + 122);
        ProxyCommunicationUtils.sendMessage(message);
        message = new ProtocolMessage(Protocol.UPDATE_PROFILE, MissionControl.getDbManager().getProxy(uuid).toString(), "remove_cosmetic", "Mission Control", uuid.toString() + "\n" + 121);
        ProxyCommunicationUtils.sendMessage(message);
        return new ArrayList<>();
    }
}

