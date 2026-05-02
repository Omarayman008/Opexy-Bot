package com.integrafty.opexy.command;

import com.integrafty.opexy.command.base.SlashCommand;
import com.integrafty.opexy.utils.EmbedUtil;
import net.dv8tion.jda.api.Permission;
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
            Button.secondary("voice_rename", "إعادة تسمية"),
            Button.secondary("voice_limit", "حد الأعضاء"),
            Button.secondary("voice_bitrate", "البيترات")
        );
        
        ActionRow row2 = ActionRow.of(
            Button.danger("voice_kick", "طرد عضو"),
            Button.success("voice_video_perm", "صلاحية الفيديو"),
            Button.success("voice_write_perm", "صلاحية الكتابة")
        );
 
        ActionRow row3 = ActionRow.of(
            Button.success("voice_speak_perm", "صلاحية التحدث"),
            Button.primary("voice_region", "تغيير المنطقة"),
            Button.primary("voice_trust", "إعطاء دخول"),
            Button.primary("voice_block", "حظر دخول")
        );
 
        ActionRow row4 = ActionRow.of(
            Button.primary("voice_ownership", "المالك الحالي"),
            Button.primary("voice_panel", "لوحة الصلاحيات")
        );
 
        ActionRow row5 = ActionRow.of(
            Button.danger("voice_request_staff", "طلب طاقم"),
            Button.danger("voice_delete", "حذف الغرفة")
        );

        Container container = EmbedUtil.containerBranded("Voice Control", "مركز التحكم في الغرف", body, EmbedUtil.BANNER_MAIN, row1, row2, row3, row4, row5);

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
