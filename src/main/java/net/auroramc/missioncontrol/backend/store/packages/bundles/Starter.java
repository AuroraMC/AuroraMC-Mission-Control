/*
 * Copyright (c) 2022-2023 AuroraMC Ltd. All Rights Reserved.
 *
 * PRIVATE AND CONFIDENTIAL - Distribution and usage outside the scope of your job description is explicitly forbidden except in circumstances where a company director has expressly given written permission to do so.
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
        MissionControl.getDbManager().addCosmetic(uuid, 8);
        MissionControl.getDbManager().addCosmetic(uuid, 121);
        MissionControl.getDbManager().addCosmetic(uuid, 122);
        MissionControl.getDbManager().addCosmetic(uuid, 209);
        MissionControl.getDbManager().addCosmetic(uuid, 347);
        MissionControl.getDbManager().addCosmetic(uuid, 508);
        MissionControl.getDbManager().addCosmetic(uuid, 602);
        MissionControl.getDbManager().addCosmetic(uuid, 701);
        MissionControl.getDbManager().addCosmetic(uuid, 801);
        MissionControl.getDbManager().addCosmetic(uuid, 901);
        MissionControl.getDbManager().addCosmetic(uuid, 1001);
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
        MissionControl.getDbManager().removeCosmetic(uuid, 8);
        MissionControl.getDbManager().removeCosmetic(uuid, 121);
        MissionControl.getDbManager().removeCosmetic(uuid, 122);
        MissionControl.getDbManager().removeCosmetic(uuid, 209);
        MissionControl.getDbManager().removeCosmetic(uuid, 347);
        MissionControl.getDbManager().removeCosmetic(uuid, 508);
        MissionControl.getDbManager().removeCosmetic(uuid, 602);
        MissionControl.getDbManager().removeCosmetic(uuid, 701);
        MissionControl.getDbManager().removeCosmetic(uuid, 801);
        MissionControl.getDbManager().removeCosmetic(uuid, 901);
        MissionControl.getDbManager().removeCosmetic(uuid, 1001);
        ProtocolMessage message = new ProtocolMessage(Protocol.UPDATE_PROFILE, MissionControl.getDbManager().getProxy(uuid).toString(), "remove_cosmetic", "Mission Control", uuid.toString() + "\n" + 122);
        ProxyCommunicationUtils.sendMessage(message);
        message = new ProtocolMessage(Protocol.UPDATE_PROFILE, MissionControl.getDbManager().getProxy(uuid).toString(), "remove_cosmetic", "Mission Control", uuid.toString() + "\n" + 121);
        ProxyCommunicationUtils.sendMessage(message);
        return new ArrayList<>();
    }
}

