package com.integrafty.opexy.command;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.time.Instant;

@Component
public class TicketCommand {
    
    public void handleSetup(SlashCommandInteractionEvent event) {
        String category = event.getOption("category").getAsString();
        
        if (!category.equalsIgnoreCase("tickets")) {
            event.reply("الفئة غير مدعومة حالياً. جرب `tickets`").setEphemeral(true).queue();
            return;
        }

        String rules = "### 📜 قـوانـيـن وشـروط الـدعـم الـفـنـي\n\n" +
                "**الاحترام المتبادل** — يرجى احترام جميع أعضاء الإدارة. أي إساءة قد تعرضك للحظر.\n\n" +
                "**تذكرة واحدة** — يرجى فتح تذكرة واحدة فقط لمشكلتك وعدم التكرار.\n\n" +
                "**الوضوح** — اشرح مشكلتك بالكامل فور فتح التذكرة لنسرع في خدمتك.\n\n" +
                "**المنشن** — يمنع عمل منشن (Ping) للإدارة داخل التذكرة، سنقوم بالرد بأقرب وقت.\n\n" +
                "يرجى اختيار القسم المناسب من الأزرار بالأسفل:";

        // Premium Container Style (V2) matching the new design
        EmbedBuilder embed = new EmbedBuilder()
            .setColor(Color.decode("#2B2D31")) // Discord background color to hide the side line
            .setDescription(rules)
            .setImage("https://i.imgur.com/u3lM7q5.jpeg"); // Placeholder for premium banner, user can change later

        ActionRow buttons = ActionRow.of(
            Button.secondary("ticket_support", "الدعم الفني").withEmoji(net.dv8tion.jda.api.entities.emoji.Emoji.fromUnicode("⚙️")),
            Button.secondary("ticket_whitelist", "التقديم (White List)").withEmoji(net.dv8tion.jda.api.entities.emoji.Emoji.fromUnicode("📜")),
            Button.secondary("ticket_hiring", "التوظيف (Hiring)").withEmoji(net.dv8tion.jda.api.entities.emoji.Emoji.fromUnicode("💼")),
            Button.secondary("ticket_complaint", "الشكاوى").withEmoji(net.dv8tion.jda.api.entities.emoji.Emoji.fromUnicode("⚠️"))
        );

        event.getChannel().sendMessageEmbeds(embed.build()).setComponents(buttons).queue();
        event.reply("✅ تم إرسال لوحة التذاكر (Ticket Panel) بنجاح!").setEphemeral(true).queue();
    }
}
