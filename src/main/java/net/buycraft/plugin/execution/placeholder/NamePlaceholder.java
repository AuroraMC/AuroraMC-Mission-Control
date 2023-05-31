/*
 * Copyright (c) 2023 AuroraMC Ltd. All Rights Reserved.
 *
 * PRIVATE AND CONFIDENTIAL - Distribution and usage outside the scope of your job description is explicitly forbidden except in circumstances where a company director has expressly given written permission to do so.
 */

package net.buycraft.plugin.execution.placeholder;

import net.buycraft.plugin.data.QueuedCommand;
import net.buycraft.plugin.data.QueuedPlayer;

import java.util.regex.Pattern;

public class NamePlaceholder implements Placeholder {
    private static final Pattern REPLACE_NAME = Pattern.compile("[{\\(<\\[](name|player|username)[}\\)>\\]]", Pattern.CASE_INSENSITIVE);

    @Override
    public String replace(String command, QueuedPlayer player, QueuedCommand queuedCommand) {
        return REPLACE_NAME.matcher(command).replaceAll(player.getName());
    }
}
