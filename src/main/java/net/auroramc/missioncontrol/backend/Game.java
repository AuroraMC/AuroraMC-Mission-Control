package net.auroramc.missioncontrol.backend;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Server types.
 */
public enum Game {

    LOBBY(80, true, "Lobby", true, new JSONObject().put("type", "lobby").put("game", "LOBBY"), MemoryAllocation.LOBBY, Collections.singletonList(Module.LOBBY)),
    EVENT(100, false, "Event", true, new JSONObject().put("type", "game").put("game", "EVENT").put("event", true).put("rotation", new JSONArray().put("EVENT")), MemoryAllocation.EVENT, Arrays.asList(Module.ENGINE, Module.GAME, Module.EVENT)),
    CRYSTAL_QUEST(10, true, "Crystal", true, new JSONObject().put("type", "game").put("game", "CRYSTAL_QUEST").put("rotation", new JSONArray().put("CRYSTAL_QUEST")), MemoryAllocation.GAME, Arrays.asList(Module.ENGINE, Module.GAME)),
    MIXED_ARCADE(16, true, "Arcade", false, new JSONObject().put("type", "game").put("game", "MIXED_ARCADE").put("rotation", new JSONArray().put("SPLEEF").put("RUNNER")), MemoryAllocation.GAME, Arrays.asList(Module.ENGINE, Module.GAME)),
    BUILD(32, false, "Build", true, new JSONObject().put("type", "build").put("game", "BUILD"), MemoryAllocation.GAME, Collections.singletonList(Module.BUILD)),
    STAFF(80, false, "Staff", true, new JSONObject().put("type", "staff").put("game", "STAFF").put("rotation", new JSONArray()), MemoryAllocation.LOBBY, Arrays.asList(Module.ENGINE, Module.GAME));

    private final int maxPlayers;
    private final boolean monitor;
    private final String serverCode;
    private final boolean enabled;
    private final JSONObject serverTypeInformation;
    private final MemoryAllocation memoryAllocation;
    private final List<Module> modules;

    Game(int maxPlayers, boolean monitor, String serverCode, boolean enabled, JSONObject serverTypeInformation, MemoryAllocation memoryAllocation, List<Module> modules) {
        this.maxPlayers = maxPlayers;
        this.monitor = monitor;
        this.serverCode = serverCode;
        this.enabled = enabled;
        this.serverTypeInformation = serverTypeInformation;
        this.memoryAllocation = memoryAllocation;
        this.modules = modules;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public boolean isMonitor() {
        return monitor;
    }

    public boolean isEnabled() {
        return enabled;
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
