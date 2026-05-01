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
        return Commands.slash("startup", "إرسال لوحة البداية والإعدادات للأعضاء");
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        if (!event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
            event.reply("❌ هذا الأمر للإدارة فقط.").setEphemeral(true).queue();
            return;
        }

        event.deferReply(true).queue();

        // 1. Server Map Embed
        String mapContent = 
            "### 🗺️ خـريـطـة الـسـيـرفـر | SERVER MAP\n" +
            "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬\n\n" +
            "┣ <#1487138386258165820> • استقبل الأعضاء الجدد بحماس\n" +
            "┃\n" +
            "┣ <#1488279212786843850> • وثق حسابك لفتح الرومات\n" +
            "┃\n" +
            "┣ <#1487138587827900486> • قوانين السيرفر والمعلومات الأساسية\n" +
            "┃\n" +
            "┣ <#14871386591478457> • آخر أخبار وتحديثات السيرفر\n" +
            "┃\n" +
            "┣ <#1487204004076191946> • أحدث مقاطع اليوتيوب الحصرية\n" +
            "┃\n" +
            "┣ <#1487140470172815574> • تابع البث المباشر الآن\n" +
            "┃\n" +
            "┣ <#1487139870433476720> • شرح مفصل للعب بالسيرفر\n" +
            "┃\n" +
            "┣ <#1487139967296864377> • شاركنا أفكارك لتطوير السيرفر\n" +
            "┃\n" +
            "┣ <#1487139736748425236> • حالة السيرفر التقنية الحالية\n" +
            "┃\n" +
            "┣ <#1487140811404611695> • شارك في تصويتات السيرفر\n" +
            "┃\n" +
            "┣ <#1488278492650143854> • دردشة خاصة للاعبي ماينكرافت\n" +
            "┃\n" +
            "┣ <#1487140532965867600> • استخدم أوامر البوت هنا\n" +
            "┃\n" +
            "┣ <#1487140589500629034> • شارك صورك ومقاطعك هنا\n" +
            "┃\n" +
            "┣ <#1487138843378323486> • اقتراحات تخص قسم الأوبكس\n" +
            "┃\n" +
            "┣ <#1487142537666760735> • جدول فعاليات السيرفر القادمة\n" +
            "┃\n" +
            "┣ <#1487142391474557069> • اقترح فعاليات جديدة ممتعة\n" +
            "┃\n" +
            "┣ <#1487142175765430393> • شات خاص أثناء الفعاليات\n" +
            "┃\n" +
            "┣ <#1487141372325531709> • روم صوتي عام للاجتماع\n" +
            "┃\n" +
            "┣ <#1487141441854767184> • روم صوتي إضافي للجميع\n" +
            "┃\n" +
            "┣ <#1486872077263835157> • لوحة تحكم إعداداتك الخاصة\n" +
            "┃\n" +
            "┣ <#1499728555754520667> • أنشئ رومتك الصوتية فوراً\n" +
            "┃\n" +
            "┗ <#1487143271586074624> • تواصل مع الإدارة مباشرة\n\n" +
            "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬";

        Container mapContainer = EmbedUtil.containerBranded("MAP", "HighCore Explorer", mapContent,
                EmbedUtil.BANNER_MAIN);

        // 2. Roles & Socials Section
        String settingsContent = "### ⚙️ الإعـدادات والـتـنـبـيـهات | SETTINGS\n" +
                "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬\n\n" +
                "**🎨 الألـوان:** اخـتـر لـونـك الـمـفـضـل (لـون واحـد فـقـط).\n" +
                "**🔔 الـتـنـبـيـهات:** اخـتـر الأقـسـام الـتـي تـرغـب بـمـتـابـعـتـهـا.\n" +
                "**🌐 الـسـوشـيـال:** تـابـعـنـا عـلـى مـنـصـات الـتـواصـل الاجـتـمـاعـي.\n\n" +
                "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬";

        // Buttons Rows
        ActionRow colorsRow = ActionRow.of(
                Button.secondary("color_red", "Soft Red"),
                Button.secondary("color_turquoise", "Turquoise"),
                Button.secondary("color_orange", "Carrot Orange"),
                Button.secondary("color_gray", "Light Gray"),
                Button.secondary("color_navy", "Midnight Navy"));
        ActionRow colorsRow2 = ActionRow.of(
                Button.secondary("color_blurple", "Blurple"),
                Button.secondary("color_asphalt", "Wet Asphalt"));

        ActionRow pingsRow = ActionRow.of(
                Button.secondary("ping_stream", "Stream"),
                Button.secondary("ping_minecraft", "Minecraft"),
                Button.secondary("ping_event", "Events"),
                Button.secondary("ping_mcserver", "MC-Server"),
                Button.secondary("ping_dcserver", "DC-Server"));
        ActionRow pingsRow2 = ActionRow.of(
                Button.secondary("ping_apply", "Staff Apply"),
                Button.secondary("support_direct", "Support"));

        ActionRow socialRow = ActionRow.of(
                Button.link("https://www.instagram.com/highcoremc", "Instagram"),
                Button.link("https://www.threads.com/@highcoremc", "Threads"),
                Button.link("https://www.youtube.com/@higcoremc", "YouTube"),
                Button.link("https://x.com/highcoremc", "X"),
                Button.link("https://www.tiktok.com/@highcoremcmc", "TikTok"));

        Container settingsContainer = EmbedUtil.containerBranded("SETTINGS", "Member Dashboard", settingsContent, null,
                colorsRow, colorsRow2, pingsRow, pingsRow2, socialRow);

        event.getChannel()
                .sendMessage(new MessageCreateBuilder().setComponents(mapContainer).useComponentsV2(true).build())
                .useComponentsV2(true).queue();
        event.getChannel()
                .sendMessage(new MessageCreateBuilder().setComponents(settingsContainer).useComponentsV2(true).build())
                .useComponentsV2(true).queue();

        event.getHook().sendMessage("✅ تم إرسال لوحة البداية بنجاح.").setEphemeral(true).queue();
    }
}
