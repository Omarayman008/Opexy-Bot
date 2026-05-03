package com.integrafty.opexy.command;

import com.integrafty.opexy.command.base.SlashCommand;
import com.integrafty.opexy.utils.EmbedUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.User;
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
public class UnbanCommand implements SlashCommand {

    private final LogManager logManager;

    @Override
    public String getName() {
        return "unban";
    }

    @Override
    public SlashCommandData getCommandData() {
        return Commands.slash("unban", "إلغاء حظر عضو")
                .addOption(OptionType.STRING, "user_id", "ID العضو المراد إلغاء حظره", true);
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        if (!event.getMember().hasPermission(Permission.BAN_MEMBERS)) {
            event.reply("❌ لا تملك صلاحية لاستخدام هذا الأمر.").setEphemeral(true).queue();
            return;
        }

        String userId = event.getOption("user_id").getAsString();
        event.deferReply().queue();

        event.getGuild().unban(User.fromId(userId)).queue(
            success -> {
                String description = String.format("### ✅ تـم إلـغـاء الـحـظـر\n\n**الـعـضـو (ID):** %s\n**الـمـسـؤول:** %s", 
                        userId, event.getMember().getAsMention());

                Container container = EmbedUtil.containerBranded("MODERATION", "User Unbanned", description, EmbedUtil.BANNER_MAIN);
                
                MessageCreateBuilder builder = new MessageCreateBuilder();
                builder.setComponents(container);
                builder.useComponentsV2(true);

                event.getHook().sendMessage(builder.build()).queue();

                // LOGGING
                String logDetails = String.format("### ✅ Action: Blacklist Revoked (Unban)\n▫️ **Target ID:** `%s`\n▫️ **Moderator:** %s\n▫️ **Channel:** %s",
                        userId, event.getMember().getAsMention(), event.getChannel().getAsMention());
                logManager.logEmbed(event.getGuild(), LogManager.LOG_MODS_CMD, 
                        EmbedUtil.createOldLogEmbed("unban", logDetails, event.getMember(), User.fromId(userId), null, EmbedUtil.SUCCESS));
            },
            error -> event.getHook().sendMessage("❌ لم يتم العثور على حظر لهذا الـ ID أو أن الرقم غير صحيح.").setEphemeral(true).queue()
        );
    }
}
