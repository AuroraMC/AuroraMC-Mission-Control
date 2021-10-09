/*
 * Copyright (c) 2021 AuroraMC Ltd. All Rights Reserved.
 */

package net.auroramc.missioncontrol.commands;

import net.auroramc.missioncontrol.Command;
import net.auroramc.missioncontrol.MissionControl;

import java.util.Collections;
import java.util.List;

public class CommandHelp extends Command {


    public CommandHelp() {
        super("help", Collections.singletonList("?"));
    }

    @Override
    public void execute(String aliasUsed, List<String> args) {
        MissionControl.getLogger().info("Available commands:\n" +
                "alpha enable - Enable the alpha network and network monitoring.\n" +
                "alpha disable - Disable the alpha network and network monitoring.\n" +
                "alpha branch <module=branch:build...> - Set the current branch of modules. NOTE: Doing this will restart the modules on the alpha network.\n" +
                "game enable <game> [network] - Enable the creation of game servers for this game.\n" +
                "game disable <game> [network] - Disable the creation of game servers for this game. NOTE: this will initiate shutdown of all game servers of this type, including those which are not monitored.\n" +
                "game monitor <game> <true|false> [network] - Enable/disable Mission Control monitoring for this game.\n" +
                "smanager enable <network> - Enable server creation on a specific network. Does not work for the test network.\n" +
                "smanager disable <network> - Disable server creation on a specific network. Any open servers remain open and need to be closed manually. Does not work for the test network.\n" +
                "smanager server create <network> <game> [server name]  [extra args] - Create a server on the network. NOTE: Servers created this way will not be closed automatically by Mission Control when no longer required.\n" +
                "smanager server close <network> <server name> - Forcefully close and delete a server on the network.\n" +
                "smanager server restart <network> <server name> - Restart a server on the network.\n" +
                "smanager proxy create <network> [extra args] - Create a proxy on the network. NOTE: Proxies created this way will not be closed automatically by Mission Control when no longer required.\n" +
                "smanager proxy close <proxy UUID> - Forcefully close and delete a proxy on the network.\n" +
                "smanager proxy restart <proxy UUID> - Restart a proxy on the network.\n" +
                "smanager stats <network> - List statistics about a specific network.\n" +
                "update <module:build...> - Update the network with the specified modules/builds.\n" +
                "\n" +
                "For any specific arguments on any of the commands, execute the command without any arguments.");
    }
}
