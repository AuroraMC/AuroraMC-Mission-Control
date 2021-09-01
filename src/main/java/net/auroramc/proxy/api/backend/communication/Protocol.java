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
    GLOBAL_MESSAGE,
    /**
     * The server is ready to be shutdown.
     */
    CONFIRM_SHUTDOWN,
    /**
     * Commands related to player parties.
     */
    PARTY,
    /**
     * Kick a player from the network for whatever reason.
     */
    KICK,
    /**
     * Update some part of the users profile.
     */
    UPDATE_PROFILE,
    /**
     * Announce something to the entire network.
     */
    ANNOUNCE,
    /**
     * Send a player to another server.
     */
    SEND,
    /**
     * Sent to all proxies when a media rank has joined/left the network.
     */
    MEDIA_RANK_JOIN_LEAVE,
    /**
     * Sent to all proxies when a staff member has joined/left the network.
     */
    STAFF_RANK_JOIN_LEAVE,
    /**
     * A server is now online load its details.
     */
    SERVER_ONLINE,
    /**
     * A server is now offline, delete it from the cache.
     */
    SERVER_OFFLINE
}
