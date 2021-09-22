package net.auroramc.missioncontrol.backend.managers;

import net.auroramc.missioncontrol.Command;
import net.auroramc.missioncontrol.MissionControl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CommandManager {

    public static void onCommand(String message) {
        ArrayList<String> args = new ArrayList<>(Arrays.asList(message.split(" ")));
        String commandLabel = args.remove(0).substring(1);
        onCommand(commandLabel, args);
    }

    private static void onCommand(String commandLabel, List<String> args) {
        Command command = MissionControl.getCommand(commandLabel);
        MissionControl.getLogger().info("Console user executed: " + commandLabel + " " + String.join(" ", args));
        if (command != null) {
            command.execute(commandLabel, args);
        } else {
            MissionControl.getLogger().info("That is an unrecognised command. Use 'help' to get a list of commands.");
        }
    }

}