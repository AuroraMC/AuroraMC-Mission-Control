/*
 * Copyright (c) 2021 AuroraMC Ltd. All Rights Reserved.
 */

package net.auroramc.missioncontrol.backend.util;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Server types.
 */
public enum Game {

    LOBBY(80, "Lobby", new JSONObject().put("type", "lobby").put("game", "LOBBY"), MemoryAllocation.LOBBY, Collections.singletonList(Module.LOBBY)),
    EVENT(100, "Event", new JSONObject().put("type", "game").put("game", "EVENT").put("event", true).put("rotation", new JSONArray().put("EVENT")), MemoryAllocation.EVENT, Arrays.asList(Module.ENGINE, Module.GAME, Module.EVENT)),
    CRYSTAL_QUEST(10, "CQ", new JSONObject().put("type", "game").put("game", "CRYSTAL_QUEST").put("rotation", new JSONArray().put("CRYSTAL_QUEST")), MemoryAllocation.GAME, Arrays.asList(Module.ENGINE, Module.GAME)),
    MIXED_ARCADE(16, "Arcade", new JSONObject().put("type", "game").put("game", "MIXED_ARCADE").put("rotation", new JSONArray().put("SPLEEF").put("RUNNER")), MemoryAllocation.GAME, Arrays.asList(Module.ENGINE, Module.GAME)),
    BUILD(32, "Build", new JSONObject().put("type", "build").put("game", "BUILD"), MemoryAllocation.GAME, Collections.singletonList(Module.BUILD)),
    STAFF(80, "Staff", new JSONObject().put("type", "staff").put("game", "STAFF").put("rotation", new JSONArray()), MemoryAllocation.LOBBY, Arrays.asList(Module.ENGINE, Module.GAME));

    private final int maxPlayers;
    private final String serverCode;
    private final JSONObject serverTypeInformation;
    private final MemoryAllocation memoryAllocation;
    private final List<Module> modules;

    Game(int maxPlayers, String serverCode, JSONObject serverTypeInformation, MemoryAllocation memoryAllocation, List<Module> modules) {
        this.maxPlayers = maxPlayers;
        this.serverCode = serverCode;
        this.serverTypeInformation = serverTypeInformation;
        this.memoryAllocation = memoryAllocation;
        this.modules = modules;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public String getServerCode() {
        return serverCode;
    }

    public JSONObject getServerTypeInformation() {
        return serverTypeInformation;
    }

    public MemoryAllocation getMemoryAllocation() {
        return memoryAllocation;
    }

    public List<Module> getModules() {
        return modules;
    }
}
