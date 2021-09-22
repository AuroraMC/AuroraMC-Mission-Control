package net.auroramc.missioncontrol;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public abstract class Command {

    private final String mainCommand;
    private final List<String> aliases;
    protected final Map<String, Command> subcommands;
    protected final Logger logger = MissionControl.getLogger();

    public Command(String mainCommand, List<String> alises) {
        this.mainCommand = mainCommand.toLowerCase();
        this.aliases = alises;
        this.subcommands = new HashMap<>();
    }
    public abstract void execute(String aliasUsed, List<String> args);

    protected void registerSubcommand(String subcommand, List<String> aliases, Command command) {
        subcommands.put(subcommand.toLowerCase(), command);
        for (String alias : aliases) {
            subcommands.put(alias.toLowerCase(), command);
        }
    }

    public String getMainCommand() {
        return mainCommand;
    }

    public Command getSubcommand(String subCommand) {
        return subcommands.get(subCommand);
    }

    public List<String> getAliases() {
        return aliases;
    }

}
