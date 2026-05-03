package com.integrafty.opexy.command.event;

import com.integrafty.opexy.command.base.MultiSlashCommand;
import com.integrafty.opexy.service.event.EventManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.util.List;

@Component
public class AuctionCommand implements MultiSlashCommand {

    private final EventManager eventManager;

    @Value("${opexy.roles.hype-manager}")
    private String hypeManagerId;

    @Value("${opexy.roles.hype-events}")
    private String hypeEventsId;

    public AuctionCommand(EventManager eventManager) {
        this.eventManager = eventManager;
    }

    @Override
    public List<SlashCommandData> getCommandDataList() {
        return List.of(Commands.slash("auction", "بدء المزاد الأعمى (Blind Auction)")
                .addOptions(new OptionData(OptionType.STRING, "prize", "الجائزة", true)
                        .addChoice("نقاط (opex)", "opex")
                        .addChoice("ميزة (Perk)", "perk")
                        .addChoice("عشوائي (Random)", "random")));
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("auction")) return;

        // Check permissions
        boolean hasRole = event.getMember().getRoles().stream()
                .anyMatch(r -> r.getId().equals(hypeManagerId) || r.getId().equals(hypeEventsId));
        
        if (!hasRole) {
            event.reply("❌ عذراً، هذا الأمر مخصص لمشرفي الفعاليات فقط.").setEphemeral(true).queue();
            return;
        }

        if (!eventManager.startGroupEvent("المزاد الأعمى")) {
            event.reply("⚠️ هناك فعالية جماعية قائمة بالفعل: **" + eventManager.getActiveEventName() + "**").setEphemeral(true).queue();
            return;
        }

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("🏆 المزاد الأعمى — Blind Auction")
                .setColor(Color.MAGENTA)
                .setDescription("تم بدء مزاد على صندوق غامض! 📦\n\n**القوانين:**\n• المزايدة تبدأ بـ 10 opex.\n• المزايدة الأعلى تفوز بمحتوى الصندوق.\n• الصندوق قد يحتوي على جوائز قيمة أو... فحم! 🪨")
                .addField("المزايد الحالي", "لا يوجد", true)
                .addField("أعلى سعر", "0 opex", true)
                .setFooter("ينتهي المزاد عند توقف المزايدات لمدة 30 ثانية.");

        event.replyEmbeds(embed.build())
                .setComponents(ActionRow.of(
                        Button.primary("bid_10", "+10"),
                        Button.primary("bid_50", "+50"),
                        Button.primary("bid_100", "+100"),
                        Button.success("bid_custom", "سعر مخصص ✏️")
                )).queue();
    }
}
