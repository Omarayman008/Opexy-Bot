package com.integrafty.opexy.command;

import com.integrafty.opexy.command.base.SlashCommand;
import com.integrafty.opexy.utils.EmbedUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.components.container.Container;
import org.springframework.stereotype.Component;

@Component
public class VoiceCommand implements SlashCommand {

    private static final String DASHBOARD_CHANNEL_ID = "1486872077263835157";

    @Override
    public String getName() {
        return "voice-setup";
    }

    @Override
    public SlashCommandData getCommandData() {
        return Commands.slash("voice-setup", "إعداد لوحة التحكم في الغرف الصوتية");
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        if (!event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
            event.reply("❌ لا تملك صلاحية لاستخدام هذا الأمر.").setEphemeral(true).queue();
            return;
        }

        String body = "### 🎙️ Voice Control Dashboard\n" +
                "Manage your temporary voice room using the modules below.\n\n" +
                "● **Security** — Lock or Hide your room\n" +
                "● **Identity** — Rename or Set member limits\n" +
                "● **Management** — Kick or Transfer ownership";

        ActionRow row1 = ActionRow.of(
            Button.secondary("voice_lock", "Lock"),
            Button.secondary("voice_unlock", "Unlock"),
            Button.secondary("voice_hide", "Hide"),
            Button.secondary("voice_unhide", "Unhide"),
            Button.secondary("voice_rename", "Rename")
        );

        ActionRow row2 = ActionRow.of(
            Button.secondary("voice_limit", "Limit"),
            Button.secondary("voice_claim", "Claim"),
            Button.secondary("voice_kick", "Kick"),
            Button.secondary("voice_permit", "Permit"),
            Button.secondary("voice_reject", "Reject")
        );

        ActionRow row3 = ActionRow.of(
            Button.secondary("voice_trust", "Trust"),
            Button.secondary("voice_untrust", "Untrust"),
            Button.secondary("voice_ghost", "Ghost"),
            Button.secondary("voice_unghost", "Unghost"),
            Button.secondary("voice_silence", "Silence")
        );

        ActionRow row4 = ActionRow.of(
            Button.secondary("voice_unsilence", "Unsilence"),
            Button.secondary("voice_transfer", "Transfer")
        );

        Container container = EmbedUtil.containerBranded("VOICE", "System Dashboard", body, EmbedUtil.BANNER_MAIN, row1, row2, row3, row4);

        MessageCreateBuilder builder = new MessageCreateBuilder();
        builder.setComponents(container);
        builder.useComponentsV2(true);

        net.dv8tion.jda.api.entities.channel.concrete.TextChannel dashboard = event.getGuild().getTextChannelById(DASHBOARD_CHANNEL_ID);
        if (dashboard != null) {
            dashboard.sendMessage(builder.build()).useComponentsV2(true).queue();
            event.reply("✅ تم إرسال لوحة التحكم بنجاح في " + dashboard.getAsMention()).setEphemeral(true).queue();
        } else {
            event.reply("❌ لم يتم العثور على روم الداشبورد المخصص.").setEphemeral(true).queue();
        }
    }
}
