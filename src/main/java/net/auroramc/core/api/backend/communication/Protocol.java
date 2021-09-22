package net.auroramc.core.api.backend.communication;

public enum Protocol {

    /**
     * Message the player
     */
    MESSAGE,
    /**
     * Shutdown the server when next free to.
     */
    SHUTDOWN,
    /**
     * Shutdown server immediately.
     */
    EMERGENCY_SHUTDOWN,
    /**
     * Sent by the server to confirm shutdown has occurred after a message from Mission Control. This is sent after all of the shutdown tasks are complete, not when the server is actually shutdown.
     */
    CONFIRM_SHUTDOWN,
    /**
     * Update the rules list.
     */
    UPDATE_RULES,
    /**
     * Update map rotation.
     */
    UPDATE_MAPS,
    /**
     * Send an update to the daemon with an updated player count.
     */
    UPDATE_PLAYER_COUNT,
    /**
     * When a server is online and now available to take connections.
     */
    SERVER_ONLINE,
    /**
     * Remove a server from the rotation. Only sent to lobby servers to remove servers from the menu.
     */
    REMOVE_SERVER,
    /**
     *
     */
    STAFF_MESSAGE,
    /**
     * Sent when the player count changes.
     */
    PLAYER_COUNT_CHANGE,
    /**
     * When the alpha network is closing.
     */
    ALPHA_CHANGE
}
