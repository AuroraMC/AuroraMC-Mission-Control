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

    LOBBY(80, "Lobby", new JSONObject().put("type", "lobby").put("game", "LOBBY").put("max_players", 80), MemoryAllocation.LOBBY, Collections.singletonList(Module.LOBBY)),
    EVENT(100, "Event", new JSONObject().put("type", "game").put("game", "EVENT").put("event", true).put("rotation", new JSONArray().put("EVENT")).put("max_players", 100).put("min_players", 50), MemoryAllocation.EVENT, Arrays.asList(Module.ENGINE, Module.GAME, Module.EVENT)),
    CRYSTAL_QUEST(10, "CQ", new JSONObject().put("type", "game").put("game", "CRYSTAL_QUEST").put("rotation", new JSONArray().put("CRYSTAL_QUEST")).put("max_players", 10).put("min_players", 8).put("enforce_limit", true), MemoryAllocation.GAME, Arrays.asList(Module.ENGINE, Module.GAME)),
    ARCADE_MODE(16, "Arcade", new JSONObject().put("type", "game").put("game", "ARCADE_MODE").put("rotation", new JSONArray().put("SPLEEF").put("FFA").put("HOTPOTATO")).put("max_players", 16).put("min_players", 8), MemoryAllocation.GAME, Arrays.asList(Module.ENGINE, Module.GAME)),
    BUILD(32, "Build", new JSONObject().put("type", "build").put("game", "BUILD").put("max_players", 32), MemoryAllocation.GAME, Collections.singletonList(Module.BUILD)),
    STAFF(80, "Staff", new JSONObject().put("type", "staff").put("game", "STAFF").put("rotation", new JSONArray()).put("max_players", 80).put("min_players", 40), MemoryAllocation.LOBBY, Arrays.asList(Module.ENGINE, Module.GAME));

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
