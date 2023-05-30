/*
 * Copyright (c) 2021-2023 AuroraMC Ltd. All Rights Reserved.
 *
 * PRIVATE AND CONFIDENTIAL - Distribution and usage outside the scope of your job description is explicitly forbidden except in circumstances where a company director has expressly given written permission to do so.
 */

package net.auroramc.missioncontrol.backend.managers;

import net.auroramc.missioncontrol.Command;
import net.auroramc.missioncontrol.MissionControl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CommandManager {

    public static void onCommand(String message) {
        MissionControl.getLogger().fine("Console user executed: " + message);
        ArrayList<String> args = new ArrayList<>(Arrays.asList(message.split(" ")));
        String commandLabel = args.remove(0);
        onCommand(commandLabel, args);
    }

    private static void onCommand(String commandLabel, List<String> args) {
        Command command = MissionControl.getCommand(commandLabel);
        if (command != null) {
            command.execute(commandLabel, args);
        } else {
            MissionControl.getLogger().info("That is an unrecognised command. Use 'help' to get a list of commands.");
        }
    }

}
