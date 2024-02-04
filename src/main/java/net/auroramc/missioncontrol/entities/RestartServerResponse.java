/*
 * Copyright (c) 2021-2024 Ethan P-B. All Rights Reserved.
 */

package net.auroramc.missioncontrol.entities;

public class RestartServerResponse {

    private final Info info;
    private final Type protocol;

    public RestartServerResponse(Info info, Type protocol) {
        this.info = info;
        this.protocol = protocol;
    }

    public Info getInfo() {
        return info;
    }

    public Type getProtocol() {
        return protocol;
    }

    public enum Type {CONFIRM_CLOSE, CONFIRM_OPEN}

}
