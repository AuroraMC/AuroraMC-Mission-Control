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
    UPDATE_PLAYER_COUNT
}
