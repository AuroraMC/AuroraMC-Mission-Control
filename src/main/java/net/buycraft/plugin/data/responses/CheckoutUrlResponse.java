/*
 * Copyright (c) 2023 AuroraMC Ltd. All Rights Reserved.
 *
 * PRIVATE AND CONFIDENTIAL - Distribution and usage outside the scope of your job description is explicitly forbidden except in circumstances where a company director has expressly given written permission to do so.
 */

package net.buycraft.plugin.data.responses;

import java.util.Objects;

public final class CheckoutUrlResponse {
    private final String url;

    public CheckoutUrlResponse(final String url) {
        this.url = url;
    }

    public String getUrl() {
        return this.url;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CheckoutUrlResponse that = (CheckoutUrlResponse) o;

        return Objects.equals(url, that.url);
    }

    @Override
    public int hashCode() {
        return url != null ? url.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "CheckoutUrlResponse(url=" + this.getUrl() + ")";
    }
}
