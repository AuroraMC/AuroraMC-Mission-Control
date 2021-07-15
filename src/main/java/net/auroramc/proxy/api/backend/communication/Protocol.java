package net.auroramc.proxy.api.backend.communication;

public enum Protocol {

    /**
     * Message the player
     */
    MESSAGE,
    /**
     * Shutdown the proxy when next free to.
     */
    SHUTDOWN,
    /**
     * Shutdown proxy immediately.
     */
    EMERGENCY_SHUTDOWN,
    /**
     * Update the rules list.
     */
    UPDATE_RULES,
    /**
     * Send an update to the daemon with an updated player count.
     */
    UPDATE_PLAYER_COUNT,
    /**
     * Send an update to the proxy to update the maintenance mode.
     */
    UPDATE_MAINTENANCE_MODE,
    /**
     * Send an update to the proxy about the MOTD.
     */
    UPDATE_MOTD,
    /**
     * Update someones status in the friends list.
     */
    UPDATE_FRIENDS,
    /**
     * Update the network wide chat slow.
     */
    UPDATE_CHAT_SLOW,
    /**
     * Update the network wide chat silence.
     */
    UPDATE_CHAT_SILENCE,
    /**
     * A message sent by an owner into global chat.
     */
    GLOBAL_MESSAGE

}
