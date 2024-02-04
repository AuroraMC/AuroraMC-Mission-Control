/*
 * Copyright (c) 2022-2024 Ethan P-B. All Rights Reserved.
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
