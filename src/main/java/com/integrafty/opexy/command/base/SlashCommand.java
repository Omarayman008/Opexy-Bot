package com.integrafty.opexy.command.base;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

public interface SlashCommand {
    String getName();
    SlashCommandData getCommandData();
    void execute(SlashCommandInteractionEvent event);
}
