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

        String body = "### 🎙️ لوحة التحكم في الغرف الصوتية\n" +
                "قم بإدارة غرفتك الصوتية المؤقتة باستخدام الخيارات أدناه.\n\n" +
                "● **الأمان** — قفل أو إخفاء الغرفة\n" +
                "● **الهوية** — تغيير الاسم أو تحديد عدد الأعضاء\n" +
                "● **الإدارة** — الطرد أو نقل الملكية";

        ActionRow row1 = ActionRow.of(
            Button.secondary("voice_rename", "Rename"),
            Button.secondary("voice_limit", "Limit"),
            Button.secondary("voice_bitrate", "Bitrate")
        );
        
        ActionRow row2 = ActionRow.of(
            Button.success("voice_permit", "Permit"),
            Button.success("voice_kick", "Kick"),
            Button.success("voice_join_perm", "Access")
        );
 
        ActionRow row3 = ActionRow.of(
            Button.success("voice_speak_perm", "Speak"),
            Button.success("voice_write_perm", "Write"),
            Button.success("voice_video_perm", "Video")
        );
 
        ActionRow row4 = ActionRow.of(
            Button.primary("voice_region", "Region"),
            Button.primary("voice_trust", "Trust"),
            Button.primary("voice_block", "Block")
        );
 
        ActionRow row5 = ActionRow.of(
            Button.primary("voice_ownership", "Owner"),
            Button.primary("voice_shop", "Shop"),
            Button.primary("voice_member_panel", "Panel")
        );
 
        ActionRow row6 = ActionRow.of(
            Button.danger("voice_request_staff", "Staff"),
            Button.danger("voice_delete", "Delete"),
            Button.danger("voice_info", "Info")
        );

        Container container = EmbedUtil.containerBranded("Voice Control", "Dashboard Center", body, EmbedUtil.BANNER_MAIN, row1, row2, row3, row4, row5, row6);

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
