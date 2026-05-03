package com.integrafty.opexy.command;

import com.integrafty.opexy.command.base.MultiSlashCommand;
import com.integrafty.opexy.utils.EmbedUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.components.container.Container;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class GeneralCommands implements MultiSlashCommand {

    @Override
    public List<SlashCommandData> getCommandDataList() {
        List<SlashCommandData> list = new ArrayList<>();

        list.add(Commands.slash("help", "OpexyBot help menu"));
        list.add(Commands.slash("ping", "Bot's latency status"));
        list.add(Commands.slash("roll", "Roll a 6-sided dice"));
        
        list.add(Commands.slash("colors", "View available color roles"));
        list.add(Commands.slash("color-set", "Set your color role by number")
                .addOption(OptionType.INTEGER, "number", "Color number from /colors", true));

        list.add(Commands.slash("translate", "Translate text to a specified language")
                .addOption(OptionType.STRING, "text", "Text to translate", true)
                .addOption(OptionType.STRING, "language", "Target language (e.g., English, Arabic)", true));

        list.add(Commands.slash("get-emojis", "Retrieve emoji details")
                .addOption(OptionType.STRING, "emoji", "Emoji to inspect", true));

        return list;
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        String name = event.getName().toLowerCase();

        switch (name) {
            case "help" -> handleHelp(event);
            case "ping" -> handlePing(event);
            case "roll" -> handleRoll(event);
            case "colors" -> handleColors(event);
            case "color-set" -> handleColorSet(event);
            case "translate" -> handleTranslate(event);
            case "get-emojis" -> handleGetEmojis(event);
        }
    }

    private void handleHelp(SlashCommandInteractionEvent event) {
        String body = """
                ### 🛡️ Moderation
                `/ban`, `/kick`, `/mute-text`, `/timeout`, `/clear`, `/role`, `/warn-add`, `/lock`, `/hide`
                
                ### 📡 Info
                `/user`, `/server`, `/avatar`, `/roles`, `/banner`, `/profile`
                
                ### ⚙️ General
                `/ping`, `/roll`, `/colors`, `/translate`, `/get-emojis`
                
                *Use `/` to see all available commands and their descriptions.*
                """;
        reply(event, EmbedUtil.containerBranded("Opexy Hub", "System Directory", body, EmbedUtil.BANNER_MAIN));
    }

    private void handlePing(SlashCommandInteractionEvent event) {
        long gatewayPing = event.getJDA().getGatewayPing();
        reply(event, EmbedUtil.containerBranded("SYSTEM", "Latency Protocol", "### 📡 Status\nGateway Ping: `" + gatewayPing + "ms`", EmbedUtil.BANNER_MAIN));
    }

    private void handleRoll(SlashCommandInteractionEvent event) {
        int res = new Random().nextInt(6) + 1;
        reply(event, EmbedUtil.containerBranded("GAME", "Dice Roll", "### 🎲 Result\nYou rolled a **" + res + "**", EmbedUtil.BANNER_MAIN));
    }

    private void handleColors(SlashCommandInteractionEvent event) {
        // Logic to find color roles. Usually roles named by number or in a category.
        // For now, listing roles that look like colors.
        List<Role> colorRoles = event.getGuild().getRoles().stream()
                .filter(r -> r.getName().matches("\\d+"))
                .sorted((a, b) -> Integer.compare(Integer.parseInt(a.getName()), Integer.parseInt(b.getName())))
                .collect(Collectors.toList());

        if (colorRoles.isEmpty()) {
            reply(event, EmbedUtil.error("NOT FOUND", "No numbered color roles detected in this server."));
            return;
        }

        StringBuilder sb = new StringBuilder("### 🎨 Available Color Roles\n");
        for (Role r : colorRoles) {
            sb.append("▫️ **").append(r.getName()).append("** — ").append(r.getAsMention()).append("\n");
        }
        reply(event, EmbedUtil.containerBranded("IDENTITY", "Color Selection", sb.toString(), EmbedUtil.BANNER_MAIN));
    }

    private void handleColorSet(SlashCommandInteractionEvent event) {
        int num = event.getOption("number").getAsInt();
        Role targetRole = event.getGuild().getRoles().stream()
                .filter(r -> r.getName().equals(String.valueOf(num)))
                .findFirst().orElse(null);

        if (targetRole == null) {
            replyEphemeral(event, EmbedUtil.error("INVALID SELECTION", "Color role #" + num + " does not exist."));
            return;
        }

        // Remove old color roles
        List<Role> oldColors = event.getMember().getRoles().stream()
                .filter(r -> r.getName().matches("\\d+"))
                .collect(Collectors.toList());
        
        for (Role r : oldColors) {
            event.getGuild().removeRoleFromMember(event.getMember(), r).queue();
        }

        event.getGuild().addRoleToMember(event.getMember(), targetRole).queue(v -> {
            reply(event, EmbedUtil.success("Identity Update", "Color role #" + num + " successfully assigned."));
        });
    }

    private void handleTranslate(SlashCommandInteractionEvent event) {
        String text = event.getOption("text").getAsString();
        String lang = event.getOption("language").getAsString();
        // Placeholder for real translation
        reply(event, EmbedUtil.containerBranded("Language", "Translation Engine", "### 🌐 Result\n*Translating to " + lang + "...*\n\n> " + text + "\n\n(AI Engine Offline)", EmbedUtil.BANNER_MAIN));
    }

    private void handleGetEmojis(SlashCommandInteractionEvent event) {
        String emoji = event.getOption("emoji").getAsString();
        reply(event, EmbedUtil.containerBranded("ASSET", "Emoji Inspection", "### 🔍 Details\nEmoji: " + emoji + "\nFormat: `<:name:id>`", EmbedUtil.BANNER_MAIN));
    }

    private void reply(SlashCommandInteractionEvent e, Container c) {
        var msg = new MessageCreateBuilder().setComponents(c).useComponentsV2(true).build();
        e.reply(msg).useComponentsV2(true).queue();
    }

    private void replyEphemeral(SlashCommandInteractionEvent e, Container c) {
        var msg = new MessageCreateBuilder().setComponents(c).useComponentsV2(true).build();
        e.reply(msg).setEphemeral(true).useComponentsV2(true).queue();
    }
}
