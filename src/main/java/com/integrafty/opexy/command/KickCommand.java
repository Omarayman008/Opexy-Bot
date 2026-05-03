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
import com.integrafty.opexy.service.LogManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KickCommand implements SlashCommand {

    private final LogManager logManager;

    @Override
    public String getName() {
        return "kick";
    }

    @Override
    public SlashCommandData getCommandData() {
        return Commands.slash("kick", "طرد عضو")
                .addOption(OptionType.USER, "user", "العضو المراد طرده", true)
                .addOption(OptionType.STRING, "reason", "السبب", false);
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        if (!event.getMember().hasPermission(Permission.KICK_MEMBERS)) {
            event.reply("❌ لا تملك صلاحية لاستخدام هذا الأمر.").setEphemeral(true).queue();
            return;
        }

        Member target = event.getOption("user").getAsMember();
        String reason = event.getOption("reason") != null ? event.getOption("reason").getAsString() : "بدون سبب";

        if (target == null) {
            event.reply("❌ لم يتم العثور على العضو.").setEphemeral(true).queue();
            return;
        }

        event.getGuild().kick(target).reason(reason).queue(
            success -> {
                String description = String.format("### 👞 Kick Notification\n\n**Target:** %s\n**Moderator:** %s\n**Reason:** %s", 
                        target.getUser().getAsTag(), event.getMember().getAsMention(), reason);

                Container container = EmbedUtil.containerBranded("MODERATION", "User Kicked", description, EmbedUtil.BANNER_MAIN);
                
                MessageCreateBuilder builder = new MessageCreateBuilder();
                builder.setComponents(container);
                builder.useComponentsV2(true);

                event.reply(builder.build()).useComponentsV2(true).queue();

                // LOGGING
                String logDetails = String.format("### 👞 Action: Manual Disconnect (Kick)\n▫️ **Target:** %s (`%s`)\n▫️ **Moderator:** %s\n▫️ **Reason:** %s\n▫️ **Channel:** %s",
                        target.getAsMention(), target.getId(), event.getMember().getAsMention(), reason, event.getChannel().getAsMention());
                logManager.logEmbed(event.getGuild(), LogManager.LOG_MODS_CMD, 
                        EmbedUtil.createOldLogEmbed("kick", logDetails, event.getMember(), target.getUser(), target, EmbedUtil.WARNING));
            },
            error -> event.reply("❌ حدث خطأ. تأكد من أن رتبتي أعلى من رتبة العضو.").setEphemeral(true).queue()
        );
    }
}
