/*
 * Copyright (c) 2021 AuroraMC Ltd. All Rights Reserved.
 */

package net.auroramc.missioncontrol.backend.util;

public enum Module {
    CORE("AuroraMC-Core", "AuroraMC-Core-Dev"),
    LOBBY("AuroraMC-Lobby", "AuroraMC-Lobby-Dev"),
    GAME("AuroraMC-Games", "AuroraMC-Games-Dev"),
    ENGINE("AuroraMC-Game-Engine", "AuroraMC-Game-Engine-Dev"),
    PROXY("AuroraMC-Proxy", "AuroraMC-Proxy-Dev"),
    BUILD("AuroraMC-Build", "AuroraMC-Build-Dev"),
    EVENT("AuroraMC-Event", "AuroraMC-Event-Dev"),
    DUELS("AuroraMC-Duels", "AuroraMC-Duels-Dev"),
    PATHFINDER("AuroraMC-Pathfinder", "AuroraMC-Pathfinder-Dev");

    private final String productionCIName,devCIName;


    Module(String productionCIName, String devCIName) {
        this.devCIName = devCIName;
        this.productionCIName = productionCIName;
    }

    public String getDevCIName() {
        return devCIName;
    }

    public String getProductionCIName() {
        return productionCIName;
    }
}
