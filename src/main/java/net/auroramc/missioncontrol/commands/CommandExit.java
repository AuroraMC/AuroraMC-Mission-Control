package net.auroramc.missioncontrol.commands;

import net.auroramc.missioncontrol.Command;
import net.auroramc.missioncontrol.NetworkManager;

import java.util.Collections;
import java.util.List;

public class CommandExit extends Command {


    public CommandExit() {
        super("exit", Collections.singletonList("quit"));
    }

    @Override
    public void execute(String aliasUsed, List<String> args) {
        NetworkManager.interrupt();
    }
}
