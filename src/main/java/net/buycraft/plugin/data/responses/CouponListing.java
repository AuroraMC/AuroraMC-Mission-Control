/*
 * Copyright (c) 2023 AuroraMC Ltd. All Rights Reserved.
 *
 * PRIVATE AND CONFIDENTIAL - Distribution and usage outside the scope of your job description is explicitly forbidden except in circumstances where a company director has expressly given written permission to do so.
 */

package net.buycraft.plugin.data.responses;

import net.buycraft.plugin.data.Coupon;

import java.util.List;
import java.util.Objects;

public final class CouponListing {
    private final List<Coupon> data;

    public CouponListing(final List<Coupon> data) {
        this.data = data;
    }

    public List<Coupon> getData() {
        return this.data;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CouponListing that = (CouponListing) o;

        return Objects.equals(data, that.data);
    }

    @Override
    public int hashCode() {
        return data != null ? data.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "CouponListing(data=" + this.getData() + ")";
    }
}
