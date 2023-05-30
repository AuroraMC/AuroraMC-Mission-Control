/*
 * Copyright (c) 2023 AuroraMC Ltd. All Rights Reserved.
 *
 * PRIVATE AND CONFIDENTIAL - Distribution and usage outside the scope of your job description is explicitly forbidden except in circumstances where a company director has expressly given written permission to do so.
 */

package net.buycraft.plugin.execution.placeholder;

import net.buycraft.plugin.data.QueuedCommand;
import net.buycraft.plugin.data.QueuedPlayer;

import java.util.ArrayList;
import java.util.List;

public class PlaceholderManager {
    private final List<Placeholder> placeholderList = new ArrayList<>();

    public void addPlaceholder(Placeholder placeholder) {
        placeholderList.add(placeholder);
    }

    public String doReplace(QueuedPlayer player, QueuedCommand command) {
        String c = command.getCommand();
        for (Placeholder placeholder : placeholderList) {
            c = placeholder.replace(c, player, command);
        }
        return c;
    }
}
