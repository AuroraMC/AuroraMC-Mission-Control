/*
 * Copyright (c) 2021-2024 Ethan P-B. All Rights Reserved.
 */

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
     * Punish a player on the network for whatever reason.
     */
    PUNISH,
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
    SERVER_OFFLINE,
    /**
     * This proxy is online and ready to accept connections.
     */
    PROXY_ONLINE,
    /**
     * When the proxy player count changes, send the update to Mission Control.
     */
    PLAYER_COUNT_CHANGE,
    /**
     * Update whether alpha is enabled or disabled.
     */
    ALPHA_UPDATE,
    /**
     * When there is an approval notification.
     */
    APPROVAL_NOTIFICATION,
    /**
     * When a report has been handled.
     */
    REPORT_NOTIFICATION
}
