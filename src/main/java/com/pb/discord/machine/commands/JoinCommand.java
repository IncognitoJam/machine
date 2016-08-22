package com.pb.discord.machine.commands;

import net.dv8tion.jda.entities.Message;

public class JoinCommand implements ICommand {

    @Override
    public String getName() {
        return "Join";
    }

    @Override
    public String[] getAliases() {
        return new String[] {"joinvoice"};
    }

    @Override
    public String getUsage() {
        return "join [channel name]\nJoins your voice channel or a channel you specify";
    }

    @Override
    public boolean runCommand(Message message) {
        return false;
    }

    @Override
    public boolean isAdminOnly() {
        return false;
    }

    @Override
    public boolean isOperatorOnly() {
        return false;
    }

}
