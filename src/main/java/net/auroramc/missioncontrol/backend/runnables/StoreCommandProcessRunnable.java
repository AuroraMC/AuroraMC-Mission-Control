/*
 * Copyright (c) 2022 AuroraMC Ltd. All Rights Reserved.
 */

package net.auroramc.missioncontrol.backend.runnables;

import com.mysql.cj.util.StringUtils;
import net.auroramc.missioncontrol.MissionControl;
import net.auroramc.missioncontrol.backend.store.CommandResponse;
import net.auroramc.missioncontrol.backend.store.PaymentProcessor;
import net.auroramc.proxy.api.backend.communication.Protocol;
import net.auroramc.proxy.api.backend.communication.ProtocolMessage;
import net.auroramc.proxy.api.backend.communication.ProxyCommunicationUtils;
import net.donationstore.commands.CommandManager;
import net.donationstore.http.WebstoreHTTPClient;
import net.donationstore.models.Command;
import net.donationstore.models.request.UpdateCommandExecutedRequest;
import net.donationstore.models.response.PaymentsResponse;
import net.donationstore.models.response.QueueResponse;
import org.apache.commons.lang3.RandomStringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class StoreCommandProcessRunnable implements Runnable {

    private final CommandManager commandManager;

    public StoreCommandProcessRunnable(CommandManager commandManager) {
        this.commandManager = commandManager;
    }

    @Override
    public void run() {
        try {
            UpdateCommandExecutedRequest updateCommandExecutedRequest = new UpdateCommandExecutedRequest();
            QueueResponse response = commandManager.getCommands();

            for (PaymentsResponse payment : response.payments) {
                UUID uuid = UUID.fromString(payment.meta.uuid);
                int id = MissionControl.getDbManager().getAuroraMCID(uuid);
                if (id < 0) {
                    id = MissionControl.getDbManager().newUser(uuid, payment.meta.user);
                }
                List<String> packages = new ArrayList<>();
                List<String> crates = new ArrayList<>();
                boolean chargeback = false;
                boolean refund = false;
                double amount = 0;
                for (Command command : payment.commands) {
                    CommandResponse response1 = PaymentProcessor.onCommand(command.command, id, uuid);
                    amount += Double.parseDouble(command.amount);
                    for (UUID uuid1 : response1.getCratesGiven()) {
                        crates.add(uuid1.toString());
                    }
                    packages.add(response1.getCommandId() + "");
                    chargeback = response1.isChargeback() || chargeback;
                    refund = response1.isRefund() || refund;
                    updateCommandExecutedRequest.getCommands().add(command.id);
                }

                if (refund) {
                    MissionControl.getDbManager().refundPayment(Integer.parseInt(payment.meta.paymentId));
                } else if (chargeback) {
                    //Issue ban and let proxy know if they're offline.
                    MissionControl.getDbManager().chargebackPayment(Integer.parseInt(payment.meta.paymentId));
                    String code = RandomStringUtils.randomAlphanumeric(8).toUpperCase();
                    MissionControl.getDbManager().issuePunishment(code, id, 26, "Forced chargeback on store purchase.", 1, System.currentTimeMillis(), -1, 1, uuid.toString());
                    if (MissionControl.getDbManager().hasActiveSession(uuid)) {
                        ProtocolMessage message = new ProtocolMessage(Protocol.PUNISH, MissionControl.getDbManager().getProxy(uuid).toString(), "ban", "Mission Control", code);
                        ProxyCommunicationUtils.sendMessage(message);
                    }
                } else {
                    if (MissionControl.getDbManager().hasActiveSession(uuid)) {
                        ProtocolMessage message = new ProtocolMessage(Protocol.MESSAGE, MissionControl.getDbManager().getProxy(uuid).toString(), uuid.toString(), "Mission Control", "store");
                        ProxyCommunicationUtils.sendMessage(message);
                    }
                    MissionControl.getDbManager().insertPayment(Integer.parseInt(payment.meta.paymentId), payment.meta.transactionId, id, amount, packages, crates);
                }
            }

            commandManager.updateCommandsToExecuted(updateCommandExecutedRequest);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

