/*
 * Copyright (c) 2021 AuroraMC Ltd. All Rights Reserved.
 */

package net.auroramc.missioncontrol.commands;

import net.auroramc.missioncontrol.Command;
import net.auroramc.missioncontrol.NetworkManager;
import net.auroramc.missioncontrol.backend.Module;
import net.auroramc.missioncontrol.entities.ServerInfo;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommandUpdate extends Command {


    public CommandUpdate() {
        super("update", Arrays.asList("updatenetwork", "networkupdate"));
    }

    @Override
    public void execute(String aliasUsed, List<String> args) {
        if (args.size() >= 1) {
            Map<Module, Integer> modulesToUpdate = new HashMap<>();
            for (String arg : args) {
                String[] moduleInfo = arg.split(":");
                if (moduleInfo.length != 2) {
                    logger.info("One of the arguments is formatted incorrectly. Please correct this error and try again.");
                    return;
                }
                Module module;
                try {
                    module = Module.valueOf(moduleInfo[0].toUpperCase());
                } catch (IllegalArgumentException e) {
                    logger.info("Module '" + moduleInfo[0] + "' does not exist.");
                    return;
                }
                int build;
                try {
                    build = Integer.parseInt(moduleInfo[1]);
                } catch (NumberFormatException e) {
                    logger.info("One of your build numbers is not in the correct format. Please fix this error and try again.");
                    return;
                }
                modulesToUpdate.put(module, build);
            }
            logger.info("Initiated update for " + modulesToUpdate.size() + " modules.");
            NetworkManager.pushUpdate(modulesToUpdate, ServerInfo.Network.MAIN);
        } else {
            logger.info("Invalid syntax. Correct syntax: update <module:build...>\n" +
                    "Modules to update should be seperated by spaces. e.g. core:25 build:20 engine:55\n" +
                    "A few notes about this command:\n" +
                    " - This command only works on the main network." +
                    " - To update the alpha network, use 'alpha branch'.\n" +
                    " - To update the test network, close the server and re-create it.\n" +
                    " - This command will initiate a restart of all applicable servers on the main network, including those which were created by admins using Mission Control.\n");
        }
    }
}
