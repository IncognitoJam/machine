package com.pb.discord.machine.commands;

import net.dv8tion.jda.entities.Message;

public interface ICommand {

    String getName();

    String[] getAliases();

    String getUsage();

    boolean runCommand(Message message);

    boolean isAdminOnly();

    boolean isOperatorOnly();

}
