/*
 * Copyright (c) 2021 AuroraMC Ltd. All Rights Reserved.
 */

package net.auroramc.missioncontrol.backend.communication.panel;

import net.auroramc.missioncontrol.MissionControl;
import net.auroramc.missioncontrol.NetworkManager;
import net.auroramc.missioncontrol.NetworkMonitorRunnable;
import net.auroramc.missioncontrol.backend.util.Game;
import net.auroramc.missioncontrol.backend.util.Module;
import net.auroramc.missioncontrol.entities.ServerInfo;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class PanelMessageHandler {

    private static ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(5);

    public static String onMessage(String message) {
        List<String> args = new ArrayList<>(Arrays.asList(message.split(";")));
        String command = args.remove(0);
        switch (command.toLowerCase()) {
            case "createserver": {
                if (args.size() >= 2) {
                    ServerInfo.Network network;
                    try {
                        network = ServerInfo.Network.valueOf(args.get(0));
                    } catch (IllegalArgumentException e) {
                        List<String> networks = new ArrayList<>();
                        for (ServerInfo.Network network2 : ServerInfo.Network.values()) {
                            networks.add(network2.name());
                        }
                        return "An invalid network was inputted.";
                    }

                    Game game;
                    try {
                        game = Game.valueOf(args.get(1));
                    } catch (IllegalArgumentException e) {
                        List<String> games = new ArrayList<>();
                        for (Game game2 : Game.values()) {
                            games.add(game2.name());
                        }
                        return "An invalid game was inputted.";
                    }

                    String serverName = null;
                    List<String> extraArgs = null;

                    if (args.size() >= 3) {
                        serverName = args.get(2);
                    }

                    if (MissionControl.getServers().get(network).containsKey(serverName)) {
                        return "That server already exists on that network.";
                    }

                    if (args.size() >= 4) {
                        extraArgs = Arrays.asList(args.get(3).split(" "));
                    }
                    if (extraArgs != null && network == ServerInfo.Network.TEST) {
                        Map<Module, Integer> moduleBuilds = new HashMap<>();
                        Map<Module, String> moduleBranches = new HashMap<>();
                        for (String arg : extraArgs) {
                            String[] moduleArg = arg.split("=");
                            String[] branchArg = moduleArg[1].split(":");

                            String branch = branchArg[0];
                            int build = Integer.parseInt(branchArg[1]);
                            Module module;
                            try {
                                module = Module.valueOf(moduleArg[0].toUpperCase());
                            } catch (IllegalArgumentException e) {
                                return "Module '" + moduleArg[0] + "' does not exist.";
                            }

                            if (!game.getModules().contains(module)) {
                                continue;
                            }

                            if (!MissionControl.getJenkinsManager().branchExists(module, branch)) {
                                return "The branch '" + branch + "' does not exist for this module. If it should exist, make sure that the CI job has been executed before trying again.";
                            }

                            if (!MissionControl.getJenkinsManager().buildExists(module, branch, build)) {
                                return "The build '" + build + "' does not exist for this module/branch. If it should exist, make sure that the CI job has been executed before trying again.";
                            }

                            moduleBranches.put(module, branch);
                            moduleBuilds.put(module, build);
                        }

                        if (moduleBranches.size() != game.getModules().size()) {
                            return "You have not specified the branch and build number for all modules. Aborting.";
                        }

                        ServerInfo info = NetworkManager.createServer(serverName, game, true, network, moduleBuilds.getOrDefault(Module.CORE, -1), moduleBranches.get(Module.CORE), moduleBuilds.getOrDefault(Module.LOBBY, -1), moduleBranches.get(Module.LOBBY), moduleBuilds.getOrDefault(Module.BUILD, -1), moduleBranches.get(Module.BUILD), moduleBuilds.getOrDefault(Module.GAME, -1), moduleBranches.get(Module.GAME), moduleBuilds.getOrDefault(Module.ENGINE, -1), moduleBranches.get(Module.ENGINE), false);
                        if (info != null) {
                            return "Server '" + serverName + "' successfully created on network '" + network.name() + "'. Please give the server time to start up properly.";
                        } else {
                            return "Server creation failed.";
                        }
                    } else {
                        if (network == ServerInfo.Network.TEST) {
                            return "You must provide the server name and branch/build details for each module the server will deploy with when creating a server on the test network.";
                        }

                        if (serverName == null) {
                            int id = NetworkMonitorRunnable.findLowestAvailableServerID(game, network);
                            serverName = game.getServerCode() + "-" + id;
                        } else if (MissionControl.getServers().get(network).containsKey(serverName)) {
                            return "That server already exists on that network.";
                        }

                        ServerInfo info = NetworkManager.createServer(serverName, game, true, network, false);
                        if (info != null) {
                            return "Server '" + serverName + "' successfully created on network '" + network.name() + "'. Please give the server time to start up properly.";
                        } else {
                            return "Server creation failed.";
                        }
                    }
                } else {
                    return "The command executed does not have correct arguments. Please try again.";
                }
            }
        }
        return "The command executed was not recognised by Mission Control. Please try again.";
    }

}
