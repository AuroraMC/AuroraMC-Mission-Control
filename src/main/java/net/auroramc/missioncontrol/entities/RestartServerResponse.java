/*
 * Copyright (c) 2021-2023 AuroraMC Ltd. All Rights Reserved.
 *
 * PRIVATE AND CONFIDENTIAL - Distribution and usage outside the scope of your job description is explicitly forbidden except in circumstances where a company director has expressly given written permission to do so.
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
