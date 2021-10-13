/*
 * Copyright (c) 2021 AuroraMC Ltd. All Rights Reserved.
 */

package net.auroramc.missioncontrol.commands;

import net.auroramc.missioncontrol.Command;
import net.auroramc.missioncontrol.NetworkManager;
import net.auroramc.missioncontrol.backend.Game;
import net.auroramc.missioncontrol.entities.ServerInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CommandGame extends Command {


    public CommandGame() {
        super("game", Collections.emptyList());
    }

    @Override
    public void execute(String aliasUsed, List<String> args) {
        if (args.size() > 0) {
            switch (args.get(0).toLowerCase()) {
                case "enable": {
                    if (args.size() == 2) {
                        Game game;
                        try {
                            game = Game.valueOf(args.get(1));
                        } catch (IllegalArgumentException e) {
                            List<String> games = new ArrayList<>();
                            for (Game game2 : Game.values()) {
                                games.add(game2.name());
                            }
                            logger.info("That is not a valid game. Valid games are:\n" +
                                    String.join("\n", games));
                            return;
                        }
                        NetworkManager.enableGame(game, null);
                        logger.info("Game '" + game.name() + "' has been enabled on all networks.");
                    } else if (args.size() == 3) {
                        Game game;
                        try {
                            game = Game.valueOf(args.get(1));
                        } catch (IllegalArgumentException e) {
                            List<String> games = new ArrayList<>();
                            for (Game game2 : Game.values()) {
                                games.add(game2.name());
                            }
                            logger.info("That is not a valid game. Valid games are:\n" +
                                    String.join("\n", games));
                            return;
                        }

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

                        if (NetworkManager.isGameEnabled(game, network)) {
                            logger.info("That game is already enabled!");
                            return;
                        }

                        NetworkManager.enableGame(game, network);
                        logger.info("Game '" + game.name() + "' has been enabled on network '" + network.name() + "'.");
                    } else {
                        logger.info("Invalid syntax. Correct syntax: game enable <game> [network]");
                    }
                    break;
                }
                case "disable": {
                    if (args.size() == 2) {
                        Game game;
                        try {
                            game = Game.valueOf(args.get(1));
                        } catch (IllegalArgumentException e) {
                            List<String> games = new ArrayList<>();
                            for (Game game2 : Game.values()) {
                                games.add(game2.name());
                            }
                            logger.info("That is not a valid game. Valid games are:\n" +
                                    String.join("\n", games));
                            return;
                        }
                        NetworkManager.disableGame(game, null);
                        logger.info("Game '" + game.name() + "' has been disabled on all networks.");
                    } else if (args.size() == 3) {
                        Game game;
                        try {
                            game = Game.valueOf(args.get(1));
                        } catch (IllegalArgumentException e) {
                            List<String> games = new ArrayList<>();
                            for (Game game2 : Game.values()) {
                                games.add(game2.name());
                            }
                            logger.info("That is not a valid game. Valid games are:\n" +
                                    String.join("\n", games));
                            return;
                        }

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

                        if (!NetworkManager.isGameEnabled(game, network)) {
                            logger.info("That game is already disabled!");
                            return;
                        }

                        NetworkManager.disableGame(game, network);
                        logger.info("Game '" + game.name() + "' has been disabled on network '" + network.name() + "'.");
                    } else {
                        logger.info("Invalid syntax. Correct syntax: game disable <game> [network]");
                    }
                    break;
                }
                case "monitor": {
                    if (args.size() == 3) {
                        Game game;
                        try {
                            game = Game.valueOf(args.get(1));
                        } catch (IllegalArgumentException e) {
                            List<String> games = new ArrayList<>();
                            for (Game game2 : Game.values()) {
                                games.add(game2.name());
                            }
                            logger.info("That is not a valid game. Valid games are:\n" +
                                    String.join("\n", games));
                            return;
                        }

                        boolean monitor = Boolean.parseBoolean(args.get(2));

                        NetworkManager.setMonitored(game, null, monitor);
                        logger.info("Monitoring for game '" + game.name() + "' has been " + ((monitor)?"enabled":"disabled") + " on all networks.");
                    } else if (args.size() == 4) {
                        Game game;
                        try {
                            game = Game.valueOf(args.get(1));
                        } catch (IllegalArgumentException e) {
                            List<String> games = new ArrayList<>();
                            for (Game game2 : Game.values()) {
                                games.add(game2.name());
                            }
                            logger.info("That is not a valid game. Valid games are:\n" +
                                    String.join("\n", games));
                            return;
                        }

                        boolean monitor = Boolean.parseBoolean(args.get(2));

                        ServerInfo.Network network;
                        try {
                            network = ServerInfo.Network.valueOf(args.get(3));
                        } catch (IllegalArgumentException e) {
                            List<String> networks = new ArrayList<>();
                            for (ServerInfo.Network network2 : ServerInfo.Network.values()) {
                                networks.add(network2.name());
                            }
                            logger.info("That is not a valid network. Valid networks are:\n" +
                                    String.join("\n", networks));
                            return;
                        }

                        if (NetworkManager.isGameMonitored(game, network) == monitor) {
                            logger.info("That game is already " + ((monitor)?"enabled":"disabled") + "!");
                            return;
                        }

                        NetworkManager.setMonitored(game, network, monitor);
                        logger.info("Monitoring for game '" + game.name() + "' has been " + ((monitor)?"enabled":"disabled") + " on network '" + network.name() + "'.");
                    } else {
                        logger.info("Invalid syntax. Correct syntax: game monitor <game> <true|false> [network]");
                    }
                    break;
                }
            }
        } else {
            logger.info("Available commands:\n" +
                    "game enable <game> [network] - Enable the creation of game servers for this game.\n" +
                    "game disable <game> [network] - Disable the creation of game servers for this game. NOTE: this will initiate shutdown of all game servers of this type, including those which are not monitored.\n" +
                    "game monitor <game> <true|false> [network] - Enable/disable Mission Control monitoring for this game.");
        }
    }
}
