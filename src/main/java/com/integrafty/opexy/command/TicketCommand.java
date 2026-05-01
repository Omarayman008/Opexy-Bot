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

        // Premium Embed Styling (V2 style interactivity)
        EmbedBuilder embed = new EmbedBuilder()
            .setTitle("🎫 مركز المساعدة والدعم الفني")
            .setDescription("مرحباً بك في مركز الدعم الخاص بـ **HighCore Mc**.\nيرجى اختيار القسم المناسب لاستفسارك من الأزرار أدناه:")
            .setColor(Color.decode("#5865F2"))
            .addField("🛠️ الدعم الفني (Support)", "للاستفسارات العامة والمشاكل التقنية", false)
            .addField("📜 التقديم (White List)", "لطلب الانضمام كعضو معتمد", false)
            .addField("💼 التوظيف (Hiring)", "للتقديم على الإدارة والفريق", false)
            .addField("⚠️ الشكاوى (Complaint)", "لرفع شكوى على عضو أو إداري", false)
            .setImage("https://i.imgur.com/placeholder_ticket_banner.png") // Placeholder for Ticket Banner
            .setFooter("HighCore System", event.getJDA().getSelfUser().getAvatarUrl())
            .setTimestamp(Instant.now());

        ActionRow buttons = ActionRow.of(
            Button.primary("ticket_support", "🛠️ دعم فني"),
            Button.success("ticket_whitelist", "📜 وايت ليست"),
            Button.secondary("ticket_hiring", "💼 توظيف"),
            Button.danger("ticket_complaint", "⚠️ شكوى")
        );

        event.getChannel().sendMessageEmbeds(embed.build()).setComponents(buttons).queue();
        event.reply("✅ تم إرسال لوحة التذاكر (Ticket Panel) بنجاح!").setEphemeral(true).queue();
    }
}
