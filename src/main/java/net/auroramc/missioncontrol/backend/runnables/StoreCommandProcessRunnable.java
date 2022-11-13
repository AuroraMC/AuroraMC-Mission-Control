/*
 * Copyright (c) 2022 AuroraMC Ltd. All Rights Reserved.
 */

package net.auroramc.missioncontrol.backend.runnables;

import com.google.common.base.Preconditions;
import net.auroramc.missioncontrol.MissionControl;
import net.auroramc.missioncontrol.backend.store.CommandResponse;
import net.auroramc.missioncontrol.backend.store.Payment;
import net.auroramc.missioncontrol.backend.store.PaymentProcessor;
import net.auroramc.proxy.api.backend.communication.Protocol;
import net.auroramc.proxy.api.backend.communication.ProtocolMessage;
import net.auroramc.proxy.api.backend.communication.ProxyCommunicationUtils;
import net.buycraft.plugin.BuyCraftAPI;
import net.buycraft.plugin.UuidUtil;
import net.buycraft.plugin.data.QueuedCommand;
import net.buycraft.plugin.data.QueuedPlayer;
import net.buycraft.plugin.data.responses.DueQueueInformation;
import net.buycraft.plugin.data.responses.QueueInformation;
import org.apache.commons.lang3.RandomStringUtils;

import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

public class StoreCommandProcessRunnable implements Runnable {

    private final BuyCraftAPI api;

    public StoreCommandProcessRunnable(BuyCraftAPI api) {
        this.api = api;
    }

    @Override
    public void run() {
        MissionControl.getLogger().log(Level.FINEST, "Retrieving store purchases.");
        QueueInformation information;
        try {
            information = api.retrieveOfflineQueue().execute().body();
            if (information == null) {
                return;
            }
        } catch (IOException e) {
            MissionControl.getLogger().log(Level.SEVERE, "Could not fetch due players queue", e);
            return;
        }

        Map<Integer, Payment> payments = new HashMap<>();

        List<Integer> toDelete = new ArrayList<>();

        for (QueuedCommand payment : information.getCommands()) {
            UUID uuid = UuidUtil.mojangUuidToJavaUuid(payment.getPlayer().getUuid());
            int id = MissionControl.getDbManager().getAuroraMCID(uuid);
            if (id < 0) {
                id = MissionControl.getDbManager().newUser(uuid, payment.getPlayer().getName());
            }

            String[] args = payment.getCommand().split(" ");
            String command = args[0];
            String transaction = args[1];
            String price = args[2];

            if (command.equals("chargeback")) {
                if (!payments.containsKey(payment.getPaymentId())) {
                    payments.put(payment.getPaymentId(), new Payment(id, uuid, payment.getPaymentId(), args[2]).chargeback());
                }
                command = transaction;
            } else if (command.equals("refund")) {
                if (!payments.containsKey(payment.getPaymentId())) {
                    payments.put(payment.getPaymentId(), new Payment(id, uuid, payment.getPaymentId(), args[2]).refund());
                }
                command = transaction;
            } else {
                if (payments.containsKey(payment.getPaymentId())) {
                    payments.get(payment.getPaymentId()).addAmount(Double.parseDouble(price));
                } else {
                    payments.put(payment.getPaymentId(), new Payment(id, uuid, payment.getPaymentId(), transaction).addAmount(Double.parseDouble(price)));
                }
            }

            toDelete.add(payment.getId());

            CommandResponse response1 = PaymentProcessor.onCommand(command, id, uuid, command.equals("chargeback"), command.equals("refund"));
            for (UUID uuid1 : response1.getCratesGiven()) {
                payments.get(payment.getPaymentId()).addCrate(uuid1.toString());
            }
            payments.get(payment.getPaymentId()).addPackage(response1.getCommandId() + "");
        }
        for (Payment payment : payments.values()) {
            if (payment.isRefund()) {
                MissionControl.getDbManager().refundPayment(payment.getPaymentId());
            } else if (payment.isChargeback()) {
                MissionControl.getDbManager().chargebackPayment(payment.getPaymentId());
                String code = RandomStringUtils.randomAlphanumeric(8).toUpperCase();
                MissionControl.getDbManager().issuePunishment(code, payment.getAmcId(), 24, "Forced chargeback on store purchase.", 1, System.currentTimeMillis(), -1, 1, payment.getUuid().toString());
                if (MissionControl.getDbManager().hasActiveSession(payment.getUuid())) {
                    ProtocolMessage message = new ProtocolMessage(Protocol.PUNISH, MissionControl.getDbManager().getProxy(payment.getUuid()).toString(), "ban", "Mission Control", code);
                    ProxyCommunicationUtils.sendMessage(message);
                }
            } else {
                MissionControl.getDbManager().insertPayment(payment.getPaymentId(), payment.getTransactionId(), payment.getAmcId(), payment.getAmount(), payment.getPackages(), payment.getCrates());
                if (MissionControl.getDbManager().hasActiveSession(payment.getUuid())) {
                    ProtocolMessage message = new ProtocolMessage(Protocol.MESSAGE, MissionControl.getDbManager().getProxy(payment.getUuid()).toString(), payment.getUuid().toString(), "Mission Control", "store");
                    ProxyCommunicationUtils.sendMessage(message);
                }
            }
        }
        try {
            api.deleteCommands(toDelete).execute();
        } catch (IOException e) {
            MissionControl.getLogger().log(Level.SEVERE, "Could not delete executed commands.", e);
        }
    }
}

