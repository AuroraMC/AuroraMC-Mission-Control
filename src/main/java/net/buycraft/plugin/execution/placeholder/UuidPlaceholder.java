/*
 * Copyright (c) 2023 AuroraMC Ltd. All Rights Reserved.
 *
 * PRIVATE AND CONFIDENTIAL - Distribution and usage outside the scope of your job description is explicitly forbidden except in circumstances where a company director has expressly given written permission to do so.
 */

package net.buycraft.plugin.execution.placeholder;

import net.buycraft.plugin.UuidUtil;
import net.buycraft.plugin.data.QueuedCommand;
import net.buycraft.plugin.data.QueuedPlayer;

import java.util.regex.Pattern;

public class UuidPlaceholder implements Placeholder {
    private static final Pattern REPLACE_UUID = Pattern.compile("[{\\(<\\[](uuid|id)[}\\)>\\]]", Pattern.CASE_INSENSITIVE);

    @Override
    public String replace(String command, QueuedPlayer player, QueuedCommand queuedCommand) {
        if (player.getUuid() == null) {
            return REPLACE_UUID.matcher(command).replaceAll(player.getName());
        }
        return REPLACE_UUID.matcher(command).replaceAll(UuidUtil.mojangUuidToJavaUuid(player.getUuid()).toString());
    }
}
