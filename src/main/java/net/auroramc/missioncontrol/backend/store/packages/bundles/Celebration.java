/*
 * Copyright (c) 2022-2024 Ethan P-B. All Rights Reserved.
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

public class Celebration extends Package {


    public Celebration() {
        super(7);
    }

    @Override
    public List<UUID> onReceive(int amcId, UUID uuid) {
        MissionControl.getDbManager().addCosmetic(uuid, 124);
        MissionControl.getDbManager().addCosmetic(uuid, 401);
        MissionControl.getDbManager().addCosmetic(uuid, 502);
        MissionControl.getDbManager().addCosmetic(uuid, 600);
        MissionControl.getDbManager().addCosmetic(uuid, 700);
        MissionControl.getDbManager().addCosmetic(uuid, 800);
        MissionControl.getDbManager().addCosmetic(uuid, 900);
        MissionControl.getDbManager().addCosmetic(uuid, 1000);
        ProtocolMessage message = new ProtocolMessage(Protocol.UPDATE_PROFILE, MissionControl.getDbManager().getProxy(uuid).toString(), "add_cosmetic", "Mission Control", uuid.toString() + "\n" + 122);
        ProxyCommunicationUtils.sendMessage(message);
        return new ArrayList<>();
    }

    @Override
    public List<UUID> onChargeback(int amcId, UUID uuid) {
        //Not eligible for refunds, and chargebacks will issue a ban anyway so there's no point to this.
        MissionControl.getDbManager().removeCosmetic(uuid, 124);
        MissionControl.getDbManager().removeCosmetic(uuid, 401);
        MissionControl.getDbManager().removeCosmetic(uuid, 502);
        MissionControl.getDbManager().removeCosmetic(uuid, 600);
        MissionControl.getDbManager().removeCosmetic(uuid, 700);
        MissionControl.getDbManager().removeCosmetic(uuid, 800);
        MissionControl.getDbManager().removeCosmetic(uuid, 900);
        MissionControl.getDbManager().removeCosmetic(uuid, 1000);
        ProtocolMessage message = new ProtocolMessage(Protocol.UPDATE_PROFILE, MissionControl.getDbManager().getProxy(uuid).toString(), "remove_cosmetic", "Mission Control", uuid.toString() + "\n" + 122);
        ProxyCommunicationUtils.sendMessage(message);
        return new ArrayList<>();
    }
}
