package com.integrafty.opexy.command.base;

import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import java.util.List;

public interface MultiSlashCommand {
    List<SlashCommandData> getCommandDataList();
    void execute(SlashCommandInteractionEvent event);
    default void onAutoComplete(CommandAutoCompleteInteractionEvent event) {}
}
