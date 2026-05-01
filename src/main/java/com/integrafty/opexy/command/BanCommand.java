package com.integrafty.opexy.command;

import com.integrafty.opexy.command.base.SlashCommand;
import com.integrafty.opexy.utils.EmbedUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.components.container.Container;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class BanCommand implements SlashCommand {

    @Override
    public String getName() {
        return "ban";
    }

    @Override
    public SlashCommandData getCommandData() {
        return Commands.slash("ban", "حظر عضو")
                .addOption(OptionType.USER, "user", "العضو المراد حظره", true)
                .addOption(OptionType.STRING, "reason", "السبب", false)
                .addOption(OptionType.INTEGER, "days", "أيام حذف الرسائل", false);
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        if (!event.getMember().hasPermission(Permission.BAN_MEMBERS)) {
            event.reply("❌ لا تملك صلاحية لاستخدام هذا الأمر.").setEphemeral(true).queue();
            return;
        }

        Member target = event.getOption("user").getAsMember();
        String reason = event.getOption("reason") != null ? event.getOption("reason").getAsString() : "بدون سبب";
        int delDays = event.getOption("days") != null ? event.getOption("days").getAsInt() : 0;

        if (target == null) {
            event.reply("❌ لم يتم العثور على العضو.").setEphemeral(true).queue();
            return;
        }

        event.getGuild().ban(target, delDays, TimeUnit.DAYS).reason(reason).queue(
            success -> {
                String description = String.format("### 🔨 Ban Notification\n\n**Target:** %s\n**Moderator:** %s\n**Reason:** %s", 
                        target.getUser().getAsTag(), event.getMember().getAsMention(), reason);

                Container container = EmbedUtil.containerBranded("MODERATION", "User Banned", description, EmbedUtil.BANNER_MAIN);
                
                MessageCreateBuilder builder = new MessageCreateBuilder();
                builder.setComponents(container);
                builder.useComponentsV2(true);

                event.reply(builder.build()).useComponentsV2(true).queue();
            },
            error -> event.reply("❌ لم أتمكن من حظر العضو. قد تكون رتبته أعلى مني.").setEphemeral(true).queue()
        );
    }
}
