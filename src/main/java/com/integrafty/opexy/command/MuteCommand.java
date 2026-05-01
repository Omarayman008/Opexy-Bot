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
public class MuteCommand implements SlashCommand {

    @Override
    public String getName() {
        return "mute";
    }

    @Override
    public SlashCommandData getCommandData() {
        return Commands.slash("mute", "كتم عضو")
                .addOption(OptionType.USER, "user", "العضو المراد كتمه", true)
                .addOption(OptionType.STRING, "time", "مدة الكتم (مثال: 10m, 1h)", true)
                .addOption(OptionType.STRING, "reason", "السبب", false);
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        if (!event.getMember().hasPermission(Permission.MODERATE_MEMBERS)) {
            event.reply("❌ لا تملك صلاحية (Timeout) لاستخدام هذا الأمر.").setEphemeral(true).queue();
            return;
        }

        Member target = event.getOption("user").getAsMember();
        String timeStr = event.getOption("time").getAsString();
        String reason = event.getOption("reason") != null ? event.getOption("reason").getAsString() : "بدون سبب";
        
        if (target == null) {
            event.reply("❌ لم يتم العثور على العضو.").setEphemeral(true).queue();
            return;
        }

        // Simple time parser (basic version)
        long duration = 1;
        TimeUnit unit = TimeUnit.HOURS;
        if (timeStr.endsWith("m")) {
            duration = Long.parseLong(timeStr.replace("m", ""));
            unit = TimeUnit.MINUTES;
        } else if (timeStr.endsWith("h")) {
            duration = Long.parseLong(timeStr.replace("h", ""));
            unit = TimeUnit.HOURS;
        } else if (timeStr.endsWith("d")) {
            duration = Long.parseLong(timeStr.replace("d", ""));
            unit = TimeUnit.DAYS;
        }

        event.getGuild().timeoutFor(target, duration, unit).reason(reason).queue(
            success -> {
                String description = String.format("### 🔇 Timeout Notification\n\n**Target:** %s\n**Moderator:** %s\n**Duration:** %s\n**Reason:** %s", 
                        target.getAsMention(), event.getMember().getAsMention(), timeStr, reason);

                Container container = EmbedUtil.containerBranded("MODERATION", "User Muted", description, EmbedUtil.BANNER_MAIN);
                
                MessageCreateBuilder builder = new MessageCreateBuilder();
                builder.setComponents(container);
                builder.useComponentsV2(true);

                event.reply(builder.build()).useComponentsV2(true).queue();
            },
            error -> event.reply("❌ حدث خطأ أثناء تطبيق الكتم. تأكد من صلاحيات البوت.").setEphemeral(true).queue()
        );
    }
}
