/*
 * Copyright (c) 2023 AuroraMC Ltd. All Rights Reserved.
 *
 * PRIVATE AND CONFIDENTIAL - Distribution and usage outside the scope of your job description is explicitly forbidden except in circumstances where a company director has expressly given written permission to do so.
 */

package net.buycraft.plugin.platform;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class PlatformInformation {
    @NotNull
    private final PlatformType type;
    @NotNull
    private final String version;

    public PlatformInformation(@NotNull final PlatformType type, @NotNull final String version) {
        this.type = Objects.requireNonNull(type);
        this.version = Objects.requireNonNull(version);
    }

    @NotNull
    public PlatformType getType() {
        return this.type;
    }

    @NotNull
    public String getVersion() {
        return this.version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PlatformInformation that = (PlatformInformation) o;

        if (type != that.type) return false;
        return version.equals(that.version);
    }

    @Override
    public int hashCode() {
        int result = type.hashCode();
        result = 31 * result + version.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "PlatformInformation(type=" + this.getType() + ", version=" + this.getVersion() + ")";
    }
}
