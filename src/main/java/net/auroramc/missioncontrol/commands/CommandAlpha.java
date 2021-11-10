/*
 * Copyright (c) 2021 AuroraMC Ltd. All Rights Reserved.
 */

package net.auroramc.missioncontrol.commands;

import net.auroramc.missioncontrol.Command;
import net.auroramc.missioncontrol.MissionControl;
import net.auroramc.missioncontrol.NetworkManager;
import net.auroramc.missioncontrol.backend.util.Module;
import net.auroramc.missioncontrol.entities.ServerInfo;

import java.util.*;

public class CommandAlpha extends Command {


    public CommandAlpha() {
        super("alpha", Collections.emptyList());
    }

    @Override
    public void execute(String aliasUsed, List<String> args) {
        if (args.size() > 0) {
            switch (args.get(0).toLowerCase()) {
                case "enable": {
                    if (NetworkManager.isAlphaEnabled()) {
                        logger.info("The alpha network is already enabled.");
                        return;
                    }
                    NetworkManager.setAlphaEnabled(true);
                    logger.info("The alpha network has been enabled.");
                    break;
                }
                case "disable": {
                    if (!NetworkManager.isAlphaEnabled()) {
                        logger.info("The alpha network is already disabled.");
                        return;
                    }
                    NetworkManager.setAlphaEnabled(false);
                    logger.info("The alpha network has been disabled.");
                    break;
                }
                case "branch": {
                    if (args.size() > 1) {
                        args.remove(0);
                        Map<Module, Integer> modulesToUpdate = new HashMap<>();
                        for (String arg : args) {
                            if (arg.matches("[A-Za-z]{1,10}=[A-Za-z0-9_-]{1,255}:[0-9]{1,10}")) {
                                String[] moduleArg = arg.split("=");
                                String[] branchArg = moduleArg[1].split(":");

                                String branch = branchArg[0];
                                int build;
                                try {
                                    build = Integer.parseInt(branchArg[1]);
                                } catch (NumberFormatException e) {
                                    logger.info("One of your build numbers is not in the correct format. Please fix this error and try again.");
                                    return;
                                }
                                Module module;
                                try {
                                    module = Module.valueOf(moduleArg[0].toUpperCase());
                                } catch (IllegalArgumentException e) {
                                    logger.info("Module '" + moduleArg[0] + "' does not exist.");
                                    return;
                                }

                                if (!MissionControl.getJenkinsManager().branchExists(module, branch)) {
                                    logger.info("The branch '" + branch + "' does not exist for this module. If it should exist, make sure that the CI job has been executed before trying again.");
                                    return;
                                }

                                if (!MissionControl.getJenkinsManager().buildExists(module, branch, build)) {
                                    logger.info("The build '" + build + "' does not exist for this module/branch. If it should exist, make sure that the CI job has been executed before trying again.");
                                    return;
                                }

                                NetworkManager.getAlphaBranches().put(module, branch);
                                modulesToUpdate.put(module, build);
                            } else {
                                logger.info("Your argument is not formatted properly. It should be formatted as: module=branch:build");
                                return;
                            }
                        }
                        NetworkManager.pushUpdate(modulesToUpdate, ServerInfo.Network.ALPHA);
                        logger.info("Builds updated and network restart initiated.");

                    } else {
                        logger.info("Invalid syntax. Correct syntax: alpha branch <module=branch:build...>");
                    }
                    break;
                }
            }
        } else {
            logger.info("Available commands:\n" +
                    "alpha enable - Enable the alpha network and network monitoring.\n" +
                    "alpha disable - Disable the alpha network and network monitoring.\n" +
                    "alpha branch <module=branch:build...> - Set the current branch of modules. NOTE: Doing this will restart the modules on the alpha network.");
        }
    }
}
