package com.integrafty.opexy.command;

import com.integrafty.opexy.command.base.SlashCommand;
import com.integrafty.opexy.utils.EmbedUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.components.container.Container;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class PurgeCommand implements SlashCommand {

    @Override
    public String getName() {
        return "clear";
    }

    @Override
    public SlashCommandData getCommandData() {
        return Commands.slash("clear", "مسح رسائل")
                .addOption(OptionType.INTEGER, "amount", "عدد الرسائل (1-100)", true);
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        if (!event.getMember().hasPermission(Permission.MESSAGE_MANAGE)) {
            event.reply("❌ لا تملك صلاحية لحذف الرسائل.").setEphemeral(true).queue();
            return;
        }

        int amount = event.getOption("amount").getAsInt();
        if (amount < 1 || amount > 100) {
            event.reply("❌ يرجى إدخال رقم بين 1 و 100.").setEphemeral(true).queue();
            return;
        }

        event.getChannel().getIterableHistory().takeAsync(amount).thenAccept(messages -> {
            event.getChannel().purgeMessages(messages);
            
            String description = String.format("### 🧹 Purge Successful\n\n**Messages Removed:** %d\n**Moderator:** %s", 
                    messages.size(), event.getMember().getAsMention());

            Container container = EmbedUtil.containerBranded("MODERATION", "Cleanup Complete", description, EmbedUtil.BANNER_MAIN);
            
            MessageCreateBuilder builder = new MessageCreateBuilder();
            builder.setComponents(container);
            builder.useComponentsV2(true);

            event.reply(builder.build()).useComponentsV2(true).queue(m -> {
                m.deleteOriginal().queueAfter(3, TimeUnit.SECONDS);
            });
        });
    }
}
