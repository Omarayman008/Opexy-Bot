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

        net.dv8tion.jda.api.entities.User targetUser = event.getOption("user").getAsUser();
        String reason = event.getOption("reason") != null ? event.getOption("reason").getAsString() : "بدون سبب";
        int delDays = event.getOption("days") != null ? Math.min(7, Math.max(0, event.getOption("days").getAsInt())) : 0;

        if (targetUser.getId().equals(event.getUser().getId())) {
            event.reply("❌ لا يمكنك حظر نفسك.").setEphemeral(true).queue();
            return;
        }

        if (targetUser.getId().equals(event.getJDA().getSelfUser().getId())) {
            event.reply("❌ لا يمكنك حظري!").setEphemeral(true).queue();
            return;
        }

        event.deferReply().queue();

        // Try to DM target before banning
        targetUser.openPrivateChannel().queue(pc -> {
            pc.sendMessage("⚠️ لقد تم حظرك من سيرفر **" + event.getGuild().getName() + "**\n**السبب:** " + reason).queue(
                s -> {}, e -> {} // Ignore DM errors
            );
        }, err -> {});

        event.getGuild().ban(targetUser, delDays, TimeUnit.DAYS).reason(reason).queue(
            success -> {
                String description = String.format("### 🔨 تـم الـحـظـر بـنـجـاح\n\n**الـعـضـو:** %s\n**الاسـم:** %s\n**الـمـسـؤول:** %s\n**الـسـبـب:** %s", 
                        targetUser.getAsMention(), targetUser.getName(), event.getMember().getAsMention(), reason);

                Container container = EmbedUtil.containerBranded("MODERATION", "User Banned", description, EmbedUtil.BANNER_MAIN);
                
                MessageCreateBuilder builder = new MessageCreateBuilder();
                builder.setComponents(container);
                builder.useComponentsV2(true);

                event.getHook().sendMessage(builder.build()).queue();
            },
            error -> event.getHook().sendMessage("❌ لم أتمكن من حظر العضو. قد تكون لديه رتبة أعلى مني أو لا أملك الصلاحيات الكافية.").setEphemeral(true).queue()
        );
    }
}
