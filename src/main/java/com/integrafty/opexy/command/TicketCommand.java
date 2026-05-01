package com.integrafty.opexy.command;

import com.integrafty.opexy.command.base.SlashCommand;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import com.integrafty.opexy.utils.EmbedUtil;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.Permission;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.time.Instant;

@Component
public class TicketCommand implements SlashCommand {

    @Override
    public String getName() {
        return "setup";
    }

    @Override
    public SlashCommandData getCommandData() {
        return Commands.slash("setup", "إعداد البوت")
                .addOption(OptionType.STRING, "category", "الفئة (channels, roles, etc)", true);
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        if (!event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
            Container denied = EmbedUtil.accessDenied();
            MessageCreateBuilder deniedBuilder = new MessageCreateBuilder();
            deniedBuilder.setComponents(denied);
            deniedBuilder.useComponentsV2(true);
            event.reply(deniedBuilder.build()).setEphemeral(true).useComponentsV2(true).queue();
            return;
        }

        String category = event.getOption("category").getAsString();
        if (category.equalsIgnoreCase("ticket") || category.equalsIgnoreCase("tickets")) {
            handleSetup(event);
        } else {
            event.reply("⏳ سيتم دعم فئات أخرى قريباً. جرب `tickets`").setEphemeral(true).queue();
        }
    }
    
    public void handleSetup(SlashCommandInteractionEvent event) {
        String rules = "### 📜 قـوانـيـن وشـروط الـدعـم الـفـنـي\n\n" +
                "**الاحترام المتبادل** — يرجى احترام جميع أعضاء الإدارة. أي إساءة قد تعرضك للحظر.\n\n" +
                "**تذكرة واحدة** — يرجى فتح تذكرة واحدة فقط لمشكلتك وعدم التكرار.\n\n" +
                "**الوضوح** — اشرح مشكلتك بالكامل فور فتح التذكرة لنسرع في خدمتك.\n\n" +
                "**المنشن** — يمنع عمل منشن (Ping) للإدارة داخل التذكرة، سنقوم بالرد بأقرب وقت.\n\n" +
                "يرجى اختيار القسم المناسب من الأزرار بالأسفل:";

        ActionRow buttons = ActionRow.of(
            Button.secondary("ticket_support", "الدعم الفني").withEmoji(net.dv8tion.jda.api.entities.emoji.Emoji.fromUnicode("⚙️")),
            Button.secondary("ticket_whitelist", "التقديم (White List)").withEmoji(net.dv8tion.jda.api.entities.emoji.Emoji.fromUnicode("📜")),
            Button.secondary("ticket_hiring", "التوظيف (Hiring)").withEmoji(net.dv8tion.jda.api.entities.emoji.Emoji.fromUnicode("💼")),
            Button.secondary("ticket_complaint", "الشكاوى").withEmoji(net.dv8tion.jda.api.entities.emoji.Emoji.fromUnicode("⚠️"))
        );

        Container container = EmbedUtil.containerBranded(
                "TICKETS", 
                "Support Center", 
                rules, 
                EmbedUtil.BANNER_TICKETS_MENU, 
                buttons
        );

        MessageCreateBuilder builder = new MessageCreateBuilder();
        builder.setComponents(container);
        builder.useComponentsV2(true);

        event.getChannel().sendMessage(builder.build()).useComponentsV2(true).queue();

        Container success = EmbedUtil.success("LOGISTICS", "تم إرسال لوحة التذاكر (Ticket Panel) بنجاح!");
        MessageCreateBuilder successBuilder = new MessageCreateBuilder();
        successBuilder.setComponents(success);
        successBuilder.useComponentsV2(true);
        
        event.reply(successBuilder.build()).setEphemeral(true).useComponentsV2(true).queue();
    }
}
