/*
 * Copyright (c) 2022 AuroraMC Ltd. All Rights Reserved.
 */

package net.auroramc.missioncontrol.backend.store;

import java.util.*;

public class PaymentProcessor {

    private final static Map<String, Package> packages;

    static {
        packages = new HashMap<>();
    }

    public static CommandResponse onCommand(String command, int user, UUID uuid) {
        boolean chargeback = false;
        boolean refund = false;
        if (command.startsWith("chargeback")) {
            chargeback = true;
            command = command.replace("chargeback ", "");
        }
        if (command.startsWith("refund")) {
            refund = true;
            command = command.replace("refund ", "");
        }

        Package aPackage = packages.get(command);

        List<UUID> crates = new ArrayList<>();
        if (chargeback || refund) {
            crates.addAll(aPackage.onChargeback(user, uuid));
        } else {
            crates.addAll(aPackage.onReceive(user, uuid));
        }
        return new CommandResponse(aPackage.packageId, crates, chargeback, refund);
    }

    public static void registerPackage(String command, Package apackage) {
        packages.put(command, apackage);
    }

}
