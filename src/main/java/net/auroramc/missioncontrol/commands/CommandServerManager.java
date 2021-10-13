/*
 * Copyright (c) 2021 AuroraMC Ltd. All Rights Reserved.
 */

package net.auroramc.missioncontrol.commands;

import net.auroramc.proxy.api.backend.communication.Protocol;
import net.auroramc.core.api.backend.communication.ServerCommunicationUtils;
import net.auroramc.missioncontrol.Command;
import net.auroramc.missioncontrol.MissionControl;
import net.auroramc.missioncontrol.NetworkManager;
import net.auroramc.missioncontrol.NetworkMonitorRunnable;
import net.auroramc.missioncontrol.backend.Game;
import net.auroramc.missioncontrol.backend.Module;
import net.auroramc.missioncontrol.entities.ProxyInfo;
import net.auroramc.missioncontrol.entities.ServerInfo;
import net.auroramc.proxy.api.backend.communication.ProtocolMessage;
import net.auroramc.proxy.api.backend.communication.ProxyCommunicationUtils;

import java.util.*;

public class CommandServerManager extends Command {


    public CommandServerManager() {
        super("smanager", Arrays.asList("sc", "server-creator","server-manager"));
    }

    @Override
    public void execute(String aliasUsed, List<String> args) {
        if (args.size() > 0) {
            switch (args.get(0).toLowerCase(Locale.ROOT)) {
                case "enable": {
                    if (args.size() == 2) {
                        ServerInfo.Network network;
                        try {
                            network = ServerInfo.Network.valueOf(args.get(1));
                        } catch (IllegalArgumentException e) {
                            List<String> networks = new ArrayList<>();
                            for (ServerInfo.Network network2 : ServerInfo.Network.values()) {
                                networks.add(network2.name());
                            }
                            logger.info("That is not a valid network. Valid networks are:\n" +
                                    String.join("\n", networks));
                            return;
                        }

                        if (network == ServerInfo.Network.TEST) {
                            logger.info("You cannot enable server monitoring on the test network.");
                            return;
                        }
                        if (NetworkManager.isServerMonitoringEnabled(network)) {
                            logger.info("Server monitoring is already enabled for network '" + network.name() + "'.");
                            return;
                        }
                        NetworkManager.setServerManagerEnabled(network, true);
                        logger.info("Server monitoring has been enabled for network '" + network.name() + "'.");

                    } else {
                        logger.info("Invalid syntax. Correct syntax: smanager enable <network>");
                    }
                    break;
                }
                case "disable": {
                    if (args.size() == 2) {
                        ServerInfo.Network network;
                        try {
                            network = ServerInfo.Network.valueOf(args.get(1));
                        } catch (IllegalArgumentException e) {
                            List<String> networks = new ArrayList<>();
                            for (ServerInfo.Network network2 : ServerInfo.Network.values()) {
                                networks.add(network2.name());
                            }
                            logger.info("That is not a valid network. Valid networks are:\n" +
                                    String.join("\n", networks));
                            return;
                        }

                        if (network == ServerInfo.Network.TEST) {
                            logger.info("You cannot disable server monitoring on the test network.");
                            return;
                        }
                        if (!NetworkManager.isServerMonitoringEnabled(network)) {
                            logger.info("Server monitoring is already disabled for network '" + network.name() + "'.");
                            return;
                        }
                        NetworkManager.setServerManagerEnabled(network, false);
                        logger.info("Server monitoring has been disabled for network '" + network.name() + "'.");

                    } else {
                        logger.info("Invalid syntax. Correct syntax: smanager disable <network>");
                    }
                    break;
                }
                case "server": {
                    if (args.size() > 1) {
                        switch (args.get(1).toLowerCase()) {
                            case "create": {
                                if (args.size() >= 4) {
                                    ServerInfo.Network network;
                                    try {
                                        network = ServerInfo.Network.valueOf(args.get(2));
                                    } catch (IllegalArgumentException e) {
                                        List<String> networks = new ArrayList<>();
                                        for (ServerInfo.Network network2 : ServerInfo.Network.values()) {
                                            networks.add(network2.name());
                                        }
                                        logger.info("That is not a valid network. Valid networks are:\n" +
                                                String.join("\n", networks));
                                        return;
                                    }

                                    Game game;
                                    try {
                                        game = Game.valueOf(args.get(3));
                                    } catch (IllegalArgumentException e) {
                                        List<String> games = new ArrayList<>();
                                        for (Game game2 : Game.values()) {
                                            games.add(game2.name());
                                        }
                                        logger.info("That is not a valid game. Valid games are:\n" +
                                                String.join("\n", games));
                                        return;
                                    }

                                    String serverName = null;
                                    List<String> extraArgs = null;

                                    if (args.size() >= 5) {
                                        serverName = args.get(4);
                                    }

                                    if (args.size() >= 6) {
                                        args.remove(0);
                                        args.remove(0);
                                        args.remove(0);
                                        args.remove(0);
                                        args.remove(0);
                                        extraArgs = args;
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
                                                logger.info("Module '" + moduleArg[0] + "' does not exist.");
                                                return;
                                            }

                                            if (!game.getModules().contains(module)) {
                                                logger.info("Module '" + moduleArg[0] + "' is not needed for this game. Skipping.");
                                                continue;
                                            }

                                            if (!MissionControl.getJenkinsManager().branchExists(module, branch)) {
                                                logger.info("The branch '" + branch + "' does not exist for this module. If it should exist, make sure that the CI job has been executed before trying again.");
                                                return;
                                            }

                                            if (!MissionControl.getJenkinsManager().buildExists(module, branch, build)) {
                                                logger.info("The build '" + build + "' does not exist for this module/branch. If it should exist, make sure that the CI job has been executed before trying again.");
                                                return;
                                            }

                                            moduleBranches.put(module, branch);
                                            moduleBuilds.put(module, build);
                                        }

                                        if (moduleBranches.size() != game.getModules().size()) {
                                            logger.info("You have not specified the branch and build number for all modules. Aborting.");
                                            return;
                                        }

                                        ServerInfo info = NetworkManager.createServer(serverName, game, true, network, moduleBuilds.getOrDefault(Module.CORE, -1), moduleBranches.get(Module.CORE), moduleBuilds.getOrDefault(Module.LOBBY, -1), moduleBranches.get(Module.LOBBY), moduleBuilds.getOrDefault(Module.BUILD, -1), moduleBranches.get(Module.BUILD), moduleBuilds.getOrDefault(Module.GAME, -1), moduleBranches.get(Module.GAME), moduleBuilds.getOrDefault(Module.ENGINE, -1), moduleBranches.get(Module.ENGINE));
                                        if (info != null) {
                                            logger.info("Server '" + serverName + "' successfully created on network '" + network.name() + "'.");
                                        } else {
                                            logger.info("Failed to create a server.");
                                        }
                                    } else {
                                        if (network == ServerInfo.Network.TEST) {
                                            logger.info("You must provide the server name and branch/build details for each module the server will deploy with when creating a server on the test network.");
                                            return;
                                        } else if (extraArgs != null) {
                                            logger.info("You cannot provide extra details about servers not on the test network. Continuing with default settings.");
                                        }

                                        if (serverName == null) {
                                            int id = NetworkMonitorRunnable.findLowestAvailableServerID(game, network);
                                            serverName = game.getServerCode() + "-" + id;
                                        }

                                        ServerInfo info = NetworkManager.createServer(serverName, game, true, network);
                                        if (info != null) {
                                            logger.info("Server '" + serverName + "' successfully created on network '" + network.name() + "'.");
                                        } else {
                                            logger.info("Failed to create a server.");
                                        }
                                    }
                                } else {
                                    logger.info("Invalid syntax. Correct syntax: smanager server create <network> <game> [server name]  [extra args]");
                                }
                                break;
                            }
                            case "close": {
                                if (args.size() == 4) {
                                    ServerInfo.Network network;
                                    try {
                                        network = ServerInfo.Network.valueOf(args.get(2));
                                    } catch (IllegalArgumentException e) {
                                        List<String> networks = new ArrayList<>();
                                        for (ServerInfo.Network network2 : ServerInfo.Network.values()) {
                                            networks.add(network2.name());
                                        }
                                        logger.info("That is not a valid network. Valid networks are:\n" +
                                                String.join("\n", networks));
                                        return;
                                    }

                                    String serverName = args.get(3);

                                    ServerInfo info = MissionControl.getServers().get(network).get(serverName);

                                    if (info == null) {
                                        logger.info("Server '" + serverName + "' does not exist on network '" + network.name() + "'.");
                                        return;
                                    }

                                    NetworkManager.removeServerFromRotation(info);
                                    net.auroramc.core.api.backend.communication.ProtocolMessage message = new net.auroramc.core.api.backend.communication.ProtocolMessage(net.auroramc.core.api.backend.communication.Protocol.EMERGENCY_SHUTDOWN, info.getName(), "close", "Mission Control", "");
                                    ServerCommunicationUtils.sendMessage(message, network);
                                } else {
                                    logger.info("Invalid syntax. Correct syntax: smanager server close <network> <server name>");
                                }
                                break;
                            }
                            case "restart": {
                                if (args.size() == 4) {
                                    ServerInfo.Network network;
                                    try {
                                        network = ServerInfo.Network.valueOf(args.get(2));
                                    } catch (IllegalArgumentException e) {
                                        List<String> networks = new ArrayList<>();
                                        for (ServerInfo.Network network2 : ServerInfo.Network.values()) {
                                            networks.add(network2.name());
                                        }
                                        logger.info("That is not a valid network. Valid networks are:\n" +
                                                String.join("\n", networks));
                                        return;
                                    }

                                    String serverName = args.get(3);

                                    ServerInfo info = MissionControl.getServers().get(network).get(serverName);

                                    if (info == null) {
                                        logger.info("Server '" + serverName + "' does not exist on network '" + network.name() + "'.");
                                        return;
                                    }

                                    NetworkManager.removeServerFromRotation(info);
                                    net.auroramc.core.api.backend.communication.ProtocolMessage message = new net.auroramc.core.api.backend.communication.ProtocolMessage(net.auroramc.core.api.backend.communication.Protocol.EMERGENCY_SHUTDOWN, info.getName(), "restart", "Mission Control", "");
                                    ServerCommunicationUtils.sendMessage(message, network);
                                } else {
                                    logger.info("Invalid syntax. Correct syntax: smanager server restart <network> <server name>");
                                }
                                break;
                            }
                            default: {
                                logger.info("Available commands:\n" +
                                        "smanager server create <network> <game> [server name]  [extra args] - Create a server on the network. NOTE: Servers created this way will not be closed automatically by Mission Control when no longer required.\n" +
                                        "smanager server close <network> <server name> - Forcefully close and delete a server on the network.\n" +
                                        "smanager server restart <network> <server name> - Restart a server on the network.");
                            }
                        }
                    } else {
                        logger.info("Available commands:\n" +
                                "smanager server create <network> <game> [server name]  [extra args] - Create a server on the network. NOTE: Servers created this way will not be closed automatically by Mission Control when no longer required.\n" +
                                "smanager server close <network> <server name> - Forcefully close and delete a server on the network.\n" +
                                "smanager server restart <network> <server name> - Restart a server on the network.");
                    }
                    break;
                }
                case "proxy": {
                    if (args.size() > 1) {
                        switch (args.get(1).toLowerCase()) {
                            case "create": {
                                if (args.size() >= 3) {
                                    ServerInfo.Network network;
                                    try {
                                        network = ServerInfo.Network.valueOf(args.get(2));
                                    } catch (IllegalArgumentException e) {
                                        List<String> networks = new ArrayList<>();
                                        for (ServerInfo.Network network2 : ServerInfo.Network.values()) {
                                            networks.add(network2.name());
                                        }
                                        logger.info("That is not a valid network. Valid networks are:\n" +
                                                String.join("\n", networks));
                                        return;
                                    }

                                    String arg = null;

                                    if (args.size() == 4) {
                                        arg = args.get(3);
                                    }
                                    if (arg != null && network == ServerInfo.Network.TEST) {
                                        String[] branchArg = arg.split(":");

                                        if (branchArg.length != 2) {
                                            logger.info("When creating proxies, the extra arg must be of format: branch:build");
                                            return;
                                        }

                                        String branch = branchArg[0];
                                        int build = Integer.parseInt(branchArg[1]);

                                        if (!MissionControl.getJenkinsManager().branchExists(Module.PROXY, branch)) {
                                            logger.info("The branch '" + branch + "' does not exist for the PROXY module. If it should exist, make sure that the CI job has been executed before trying again.");
                                            return;
                                        }

                                        if (!MissionControl.getJenkinsManager().buildExists(Module.PROXY, branch, build)) {
                                            logger.info("The build '" + build + "' does not exist for the PROXY module on this branch. If it should exist, make sure that the CI job has been executed before trying again.");
                                            return;
                                        }

                                        ProxyInfo info = NetworkManager.createProxy(network, true, build, branch);
                                        if (info != null) {
                                            logger.info("Proxy '" + info.getUuid().toString() + "' successfully created on network '" + network.name() + "'.");
                                        } else {
                                            logger.info("Failed to create a proxy.");
                                        }
                                    } else {
                                        if (network == ServerInfo.Network.TEST) {
                                            logger.info("You must provide the branch/build details for the PROXY module the proxy will deploy with when creating a proxy on the test network.");
                                            return;
                                        } else if (arg != null) {
                                            logger.info("You cannot provide extra details about proxies not on the test network. Continuing with default settings.");
                                        }

                                        ProxyInfo info = NetworkManager.createProxy(network, true);
                                        if (info != null) {
                                            logger.info("Proxy '" + info.getUuid().toString() + "' successfully created on network '" + network.name() + "'.");
                                        } else {
                                            logger.info("Failed to create a proxy.");
                                        }
                                    }
                                } else {
                                    logger.info("Invalid syntax. Correct syntax: smanager proxy create <network> [extra arg]");
                                }
                                break;
                            }
                            case "close": {
                                if (args.size() == 3) {
                                    UUID uuid;

                                    try {
                                        uuid = UUID.fromString(args.get(2));
                                    } catch (IllegalArgumentException e) {
                                        logger.info("That is not a valid UUID.");
                                        return;
                                    }

                                    ProxyInfo info = MissionControl.getProxies().get(uuid);

                                    if (info == null) {
                                        logger.info("Proxy '" + uuid + "' does not exist.");
                                        return;
                                    }

                                    NetworkManager.removeProxyFromRotation(info);
                                    ProtocolMessage message = new ProtocolMessage(Protocol.EMERGENCY_SHUTDOWN, uuid.toString(), "close", "Mission Control", "");
                                    ProxyCommunicationUtils.sendMessage(message);
                                    logger.info("Shutting down proxy '" + info.getUuid() + "'");
                                } else {
                                    logger.info("Invalid syntax. Correct syntax: smanager proxy close <server name>");
                                }
                                break;
                            }
                            case "restart": {
                                if (args.size() == 3) {
                                    UUID uuid;

                                    try {
                                        uuid = UUID.fromString(args.get(2));
                                    } catch (IllegalArgumentException e) {
                                        logger.info("That is not a valid UUID.");
                                        return;
                                    }

                                    ProxyInfo info = MissionControl.getProxies().get(uuid);

                                    if (info == null) {
                                        logger.info("Proxy '" + uuid + "' does not exist.");
                                        return;
                                    }

                                    ProtocolMessage message = new ProtocolMessage(Protocol.EMERGENCY_SHUTDOWN, uuid.toString(), "restart", "Mission Control", "");
                                    ProxyCommunicationUtils.sendMessage(message);
                                } else {
                                    logger.info("Invalid syntax. Correct syntax: smanager restart close <server name>");
                                }
                                break;
                            }
                            default: {
                                logger.info("Available commands:\n" +
                                        "smanager proxy create <network> [extra arg] - Create a proxy on the network. NOTE: Proxies created this way will not be closed automatically by Mission Control when no longer required.\n" +
                                        "smanager proxy close <proxy UUID> - Forcefully close and delete a proxy on the network.\n" +
                                        "smanager proxy restart <proxy UUID> - Restart a proxy on the network.\n");
                            }
                        }
                    } else {
                        logger.info("Available commands:\n" +
                                "smanager proxy create <network> [extra arg] - Create a proxy on the network. NOTE: Proxies created this way will not be closed automatically by Mission Control when no longer required.\n" +
                                "smanager proxy close <proxy UUID> - Forcefully close and delete a proxy on the network.\n" +
                                "smanager proxy restart <proxy UUID> - Restart a proxy on the network.\n");
                    }
                    break;
                }
                case "stats": {
                    if (args.size() == 2) {
                        //Network statistics
                        ServerInfo.Network network;
                        try {
                            network = ServerInfo.Network.valueOf(args.get(1));
                        } catch (IllegalArgumentException e) {
                            List<String> networks = new ArrayList<>();
                            for (ServerInfo.Network network2 : ServerInfo.Network.values()) {
                                networks.add(network2.name());
                            }
                            logger.info("That is not a valid network. Valid networks are:\n" +
                                    String.join("\n", networks));
                            return;
                        }

                        logger.info(String.format("There are currently %s servers, %s proxies open with %s players online on network '%s'.", MissionControl.getServers().get(network).size(), MissionControl.getProxies().values().stream().filter(proxyInfo -> proxyInfo.getNetwork() == network).count(), NetworkManager.getNetworkPlayerTotal().get(network), network.name()));

                        String leftAlignFormat = "| %-15s | %-7d | %-7d |%n";
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append(String.format("Game Statistics:%n+-----------------+---------+---------+%n"));
                        stringBuilder.append(String.format("| Game            | Servers | Players |%n"));
                        stringBuilder.append(String.format("+-----------------+---------+---------+%n"));
                        for (Game game : Game.values()) {
                            stringBuilder.append(String.format(leftAlignFormat, game.name(), MissionControl.getServers().get(network).values().stream().filter(serverInfo -> serverInfo.getServerType().getString("game").equalsIgnoreCase(game.name())).count(), NetworkManager.getGamePlayerTotals().get(network).get(game)));
                        }
                        stringBuilder.append(String.format("+-----------------+---------+---------+%n"));
                        logger.info(stringBuilder.toString());

                    } else if (args.size() == 1) {
                        //General network stats.
                        int networkTotal = 0,serversTotal = 0;
                        for (ServerInfo.Network network : ServerInfo.Network.values()) {
                            networkTotal += NetworkManager.getNetworkPlayerTotal().get(network);
                            serversTotal += MissionControl.getServers().get(network).size();
                        }
                        logger.info(String.format("There are currently %s servers, %s proxies open with %s players online on 3 networks.", serversTotal, MissionControl.getProxies().size(), networkTotal));
                        String leftAlignFormat = "| %-15s | %-6d | %-6d | %-6d |%n";
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append(String.format("Game Player Totals:%n+-----------------+--------+--------+--------+%n"));
                        stringBuilder.append(String.format("| Game            | Main   | Alpha  | Test   |%n"));
                        stringBuilder.append(String.format("+-----------------+--------+--------+--------+%n"));
                        for (Game game : Game.values()) {
                            stringBuilder.append(String.format(leftAlignFormat, game.name(), NetworkManager.getGamePlayerTotals().get(ServerInfo.Network.MAIN).get(game), NetworkManager.getGamePlayerTotals().get(ServerInfo.Network.ALPHA).get(game), NetworkManager.getGamePlayerTotals().get(ServerInfo.Network.TEST).get(game)));
                        }
                        stringBuilder.append(String.format("+-----------------+--------+--------+--------+%n"));
                        logger.info(stringBuilder.toString());

                        stringBuilder = new StringBuilder();
                        stringBuilder.append(String.format("Game Server Totals:%n+-----------------+--------+--------+--------+%n"));
                        stringBuilder.append(String.format("| Game            | Main   | Alpha  | Test   |%n"));
                        stringBuilder.append(String.format("+-----------------+--------+--------+--------+%n"));
                        for (Game game : Game.values()) {
                            stringBuilder.append(String.format(leftAlignFormat, game.name(), MissionControl.getServers().get(ServerInfo.Network.MAIN).values().stream().filter(serverInfo -> serverInfo.getServerType().getString("game").equalsIgnoreCase(game.name())).count(), MissionControl.getServers().get(ServerInfo.Network.ALPHA).values().stream().filter(serverInfo -> serverInfo.getServerType().getString("game").equalsIgnoreCase(game.name())).count(), MissionControl.getServers().get(ServerInfo.Network.TEST).values().stream().filter(serverInfo -> serverInfo.getServerType().getString("game").equalsIgnoreCase(game.name())).count()));
                        }
                        stringBuilder.append(String.format("+-----------------+--------+--------+--------+%n"));
                        logger.info(stringBuilder.toString());
                    }
                    break;
                }
                default: {
                    logger.info("Available commands:\n" +
                            "smanager enable <network> - Enable server creation on a specific network. Does not work for the test network.\n" +
                            "smanager disable <network> - Disable server creation on a specific network. Any open servers remain open and need to be closed manually. Does not work for the test network.\n" +
                            "smanager server create <network> <game> [server name]  [extra args] - Create a server on the network. NOTE: Servers created this way will not be closed automatically by Mission Control when no longer required.\n" +
                            "smanager server close <network> <server name> - Forcefully close and delete a server on the network.\n" +
                            "smanager server restart <network> <server name> - Restart a server on the network.\n" +
                            "smanager proxy create <network> [extra arg] - Create a proxy on the network. NOTE: Proxies created this way will not be closed automatically by Mission Control when no longer required.\n" +
                            "smanager proxy close <proxy UUID> - Forcefully close and delete a proxy on the network.\n" +
                            "smanager proxy restart <proxy UUID> - Restart a proxy on the network.\n" +
                            "smanager stats <network> - List statistics about a specific network.");
                }
            }
        } else {
            logger.info("Available commands:\n" +
                    "smanager enable <network> - Enable server creation on a specific network. Does not work for the test network.\n" +
                    "smanager disable <network> - Disable server creation on a specific network. Any open servers remain open and need to be closed manually. Does not work for the test network.\n" +
                    "smanager server create <network> <game> [server name]  [extra args] - Create a server on the network. NOTE: Servers created this way will not be closed automatically by Mission Control when no longer required.\n" +
                    "smanager server close <network> <server name> - Forcefully close and delete a server on the network.\n" +
                    "smanager server restart <network> <server name> - Restart a server on the network.\n" +
                    "smanager proxy create <network> [extra args] - Create a proxy on the network. NOTE: Proxies created this way will not be closed automatically by Mission Control when no longer required.\n" +
                    "smanager proxy close <proxy UUID> - Forcefully close and delete a proxy on the network.\n" +
                    "smanager proxy restart <proxy UUID> - Restart a proxy on the network.\n" +
                    "smanager stats <network> - List statistics about a specific network.");
        }
    }
}
