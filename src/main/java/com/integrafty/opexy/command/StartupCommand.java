package com.integrafty.opexy.command;

import com.integrafty.opexy.command.base.SlashCommand;
import com.integrafty.opexy.utils.EmbedUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import org.springframework.stereotype.Component;

@Component
public class StartupCommand implements SlashCommand {

    @Override
    public String getName() {
        return "startup";
    }

    @Override
    public SlashCommandData getCommandData() {
        return Commands.slash("startup", "Send main dashboard to members");
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        if (!event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
            event.reply("X Administrator only.").setEphemeral(true).queue();
            return;
        }

        String body = "### MAIN DASHBOARD\n" +
                "--------------------------\n\n" +
                "Welcome to **HighCore MC**.\n" +
                "Use buttons below to access Server Map, Colors, Pings, or Social Media.\n\n" +
                "--------------------------";

        ActionRow row = ActionRow.of(
            Button.secondary("startup_map", "Server Map"),
            Button.secondary("startup_colors", "Colors"),
            Button.secondary("startup_pings", "Pings"),
            Button.secondary("startup_socials", "Social Media")
        );

        Container container = EmbedUtil.containerBranded("STARTUP", "HighCore MC - Welcome", body, EmbedUtil.BANNER_MAIN, row);

        MessageCreateBuilder builder = new MessageCreateBuilder();
        builder.setComponents(container);
        builder.useComponentsV2(true);

        event.getChannel().sendMessage(builder.build()).useComponentsV2(true).queue();
        event.reply("Main dashboard sent successfully.").setEphemeral(true).queue();
    }
}
