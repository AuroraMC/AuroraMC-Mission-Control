/*
 * Copyright (c) 2022-2023 AuroraMC Ltd. All Rights Reserved.
 *
 * PRIVATE AND CONFIDENTIAL - Distribution and usage outside the scope of your job description is explicitly forbidden except in circumstances where a company director has expressly given written permission to do so.
 */

package net.auroramc.missioncontrol.backend.store;

import java.util.*;

public class PaymentProcessor {

    private final static Map<String, Package> packages;

    static {
        packages = new HashMap<>();
    }

    public static CommandResponse onCommand(String command, int user, UUID uuid, boolean chargeback, boolean refund) {
        Package aPackage = packages.get(command);

        if (aPackage == null) {
            return new CommandResponse(-1, new ArrayList<>());
        }

        List<UUID> crates = new ArrayList<>();
        if (chargeback || refund) {
            crates.addAll(aPackage.onChargeback(user, uuid));
        } else {
            crates.addAll(aPackage.onReceive(user, uuid));
        }
        return new CommandResponse(aPackage.packageId, crates);
    }

    public static void registerPackage(String command, Package apackage) {
        packages.put(command, apackage);
    }

}
