/*
 * Copyright (c) 2022-2023 AuroraMC Ltd. All Rights Reserved.
 *
 * PRIVATE AND CONFIDENTIAL - Distribution and usage outside the scope of your job description is explicitly forbidden except in circumstances where a company director has expressly given written permission to do so.
 */

package net.auroramc.missioncontrol.backend.store;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Payment {

    private final int amcId;
    private final UUID uuid;
    private final int paymentId;
    private final String transactionId;
    private final List<String> packages;
    private double amount;
    private boolean refund;
    private boolean chargeback;
    private final List<String> crates;

    public Payment(int amcId, UUID uuid, int paymentId, String transactionId) {
        this.paymentId = paymentId;
        this.transactionId = transactionId;
        this.amount = 0.0;
        this.packages = new ArrayList<>();
        this.chargeback = false;
        this.refund = false;
        this.amcId = amcId;
        this.uuid = uuid;
        this.crates = new ArrayList<>();
    }

    public int getPaymentId() {
        return paymentId;
    }

    public double getAmount() {
        return amount;
    }

    public List<String> getPackages() {
        return packages;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public Payment addAmount(double amount) {
        this.amount += amount;
        return this;
    }

    public Payment addPackage(String package1) {
        this.packages.add(package1);
        return this;
    }

    public Payment addCrate(String crate) {
        this.crates.add(crate);
        return this;
    }

    public boolean isChargeback() {
        return chargeback;
    }

    public boolean isRefund() {
        return refund;
    }

    public Payment chargeback() {
        this.chargeback = true;
        return this;
    }

    public Payment refund() {
        this.refund = true;
        return this;
    }

    public int getAmcId() {
        return amcId;
    }

    public UUID getUuid() {
        return uuid;
    }

    public List<String> getCrates() {
        return crates;
    }
}
