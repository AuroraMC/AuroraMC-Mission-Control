/*
 * Copyright (c) 2023 AuroraMC Ltd. All Rights Reserved.
 *
 * PRIVATE AND CONFIDENTIAL - Distribution and usage outside the scope of your job description is explicitly forbidden except in circumstances where a company director has expressly given written permission to do so.
 */

package net.buycraft.plugin.platform.standalone.runner;

/**
 * {@code CommandDispatcher}s are called when Buycraft processes a command. The dispatcher will not get any other
 * information from the command.
 */
public interface CommandDispatcher {
    void dispatchCommand(String command);
}
