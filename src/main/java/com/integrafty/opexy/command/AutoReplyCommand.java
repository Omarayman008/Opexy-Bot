package com.integrafty.opexy.command;

import com.integrafty.opexy.command.base.SlashCommand;
import com.integrafty.opexy.service.AutoReplyService;
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

import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AutoReplyCommand implements SlashCommand {

    private final AutoReplyService autoReplyService;

    @Override
    public String getName() { return "autoreply"; }

    @Override
    public SlashCommandData getCommandData() {
        return Commands.slash("autoreply", "Manage auto-reply triggers")
                .addOption(OptionType.STRING, "action", "add / remove / list", true)
                .addOption(OptionType.STRING, "keyword", "Trigger keyword", false)
                .addOption(OptionType.STRING, "response", "Response text", false);
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
                String keyword = event.getOption("keyword") != null ? event.getOption("keyword").getAsString() : null;
                String response = event.getOption("response") != null ? event.getOption("response").getAsString() : null;

                if (keyword == null || response == null) {
                    reply(event, EmbedUtil.error("Missing Fields", "You must provide both `keyword` and `response`."), true);
                    return;
                }

                autoReplyService.addResponse(keyword, response, event.getUser().getName());
                reply(event, EmbedUtil.success("Auto-Reply Added", "Trigger: `" + keyword + "` → `" + response + "`"), true);
            }
            case "remove" -> {
                String keyword = event.getOption("keyword") != null ? event.getOption("keyword").getAsString() : null;

                if (keyword == null) {
                    reply(event, EmbedUtil.error("Missing Keyword", "Provide the keyword to remove."), true);
                    return;
                }

                autoReplyService.removeResponse(keyword);
                reply(event, EmbedUtil.success("Auto-Reply Removed", "Trigger `" + keyword + "` has been deleted."), true);
            }
            case "list" -> {
                Map<String, String> all = autoReplyService.getAllResponses();
                if (all.isEmpty()) {
                    reply(event, EmbedUtil.info("Auto-Replies", "No auto-replies configured yet."), true);
                    return;
                }
                String list = all.entrySet().stream()
                        .map(e -> "▸ `" + e.getKey() + "` → " + e.getValue())
                        .collect(Collectors.joining("\n"));
                reply(event, EmbedUtil.containerBranded("AUTO-REPLY", "Configured Triggers", list, EmbedUtil.BANNER_MAIN), true);
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
