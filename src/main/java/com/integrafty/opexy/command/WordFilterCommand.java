package com.integrafty.opexy.command;

import com.integrafty.opexy.command.base.SlashCommand;
import com.integrafty.opexy.service.WordFilterService;
import com.integrafty.opexy.utils.EmbedUtil;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.components.container.Container;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class WordFilterCommand implements SlashCommand {

    private final WordFilterService wordFilterService;

    @Override
    public String getName() { return "wordfilter"; }

    @Override
    public SlashCommandData getCommandData() {
        return Commands.slash("wordfilter", "Manage the word filter list")
                .addOption(OptionType.STRING, "action", "add / remove / list", true)
                .addOption(OptionType.STRING, "word", "The word to add or remove", false);
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        if (!event.getMember().hasPermission(Permission.MANAGE_SERVER)) {
            reply(event, EmbedUtil.accessDenied(), true);
            return;
        }

        String action = event.getOption("action").getAsString().toLowerCase();

        switch (action) {
            case "add" -> {
                String word = event.getOption("word") != null ? event.getOption("word").getAsString() : null;

                if (word == null) {
                    reply(event, EmbedUtil.error("Missing Word", "Provide the word to add to the filter."), true);
                    return;
                }

                wordFilterService.addWord(word);
                reply(event, EmbedUtil.success("Word Added", "The word `" + word + "` is now blocked."), true);
            }
            case "remove" -> {
                String word = event.getOption("word") != null ? event.getOption("word").getAsString() : null;

                if (word == null) {
                    reply(event, EmbedUtil.error("Missing Word", "Provide the word to remove from the filter."), true);
                    return;
                }

                wordFilterService.removeWord(word);
                reply(event, EmbedUtil.success("Word Removed", "The word `" + word + "` is no longer blocked."), true);
            }
            case "list" -> {
                java.util.Set<String> all = wordFilterService.getAllWords();
                if (all.isEmpty()) {
                    reply(event, EmbedUtil.info("Word Filter", "No words in the filter list."), true);
                    return;
                }
                String list = all.stream()
                        .map(w -> "▸ `" + w + "`")
                        .collect(Collectors.joining("\n"));
                reply(event, EmbedUtil.containerBranded("WORD FILTER", "Blocked Words", list, EmbedUtil.BANNER_MAIN), true);
            }
            default -> reply(event, EmbedUtil.error("Invalid Action", "Use `add`, `remove`, or `list`."), true);
        }
    }

    private void reply(SlashCommandInteractionEvent event, Container container, boolean ephemeral) {
        MessageCreateBuilder builder = new MessageCreateBuilder();
        builder.setComponents(container);
        builder.useComponentsV2(true);
        event.reply(builder.build()).setEphemeral(ephemeral).useComponentsV2(true).queue();
    }
}
