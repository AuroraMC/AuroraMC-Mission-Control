/*
 * Copyright (c) 2021 AuroraMC Ltd. All Rights Reserved.
 */

package net.auroramc.pathfinder.missioncontrol.backend.communication;

public enum Protocol {

    /**
     * Send the player to a specific server
     */
    SEND,
    /**
     * Tell Mission Control that a server is to be closed when next available to.
     */
    SHUTDOWN,
    /**
     * Tell Mission Control that a server is to be closed immediately.
     */
    EMERGENCY_SHUTDOWN,
    /**
     * When a server is online and now available to take connections. Mission Control deals with letting the proxies know.
     */
    SERVER_ONLINE,
    /**
     * Create a server.
     */
    CREATE_SERVER,
    /**
     * Confirm the last message.
     */
    CONFIRM,
    /**
     * When a player first joins the Pathfinder Gamemode.
     */
    PLAYER_JOIN,
    /**
     * When a player leaves the pathfinder gamemode, prompts all servers to save the data.
     */
    PLAYER_LEAVE
}
