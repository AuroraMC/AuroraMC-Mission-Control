/*
 * Copyright (c) 2022 AuroraMC Ltd. All Rights Reserved.
 */

package net.auroramc.missioncontrol.backend.store.packages.crates;

import net.auroramc.missioncontrol.MissionControl;
import net.auroramc.missioncontrol.backend.store.Package;
import net.auroramc.missioncontrol.backend.util.CrateUtil;
import net.auroramc.proxy.api.backend.communication.Protocol;
import net.auroramc.proxy.api.backend.communication.ProtocolMessage;
import net.auroramc.proxy.api.backend.communication.ProxyCommunicationUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Gold5 extends Package {


    public Gold5() {
        super(10);
    }

    @Override
    public List<UUID> onReceive(int amcId, UUID uuid) {
        List<UUID> uuids = new ArrayList<>();
        for (int i = 0;i < 5;i++) {
            uuids.add(CrateUtil.generateGoldCrate(amcId));
        }
        return uuids;
    }

    @Override
    public List<UUID> onChargeback(int amcId, UUID uuid) {
        //Not eligible for refunds, and chargebacks will issue a ban anyway so there's no point to this.
        return new ArrayList<>();
    }
}
