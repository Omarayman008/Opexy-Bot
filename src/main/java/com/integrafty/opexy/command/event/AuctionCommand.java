package com.integrafty.opexy.command.event;

import com.integrafty.opexy.command.base.MultiSlashCommand;
import com.integrafty.opexy.service.event.AuctionManager;
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
    private final AuctionManager auctionManager;

    @Value("${opexy.roles.hype-manager}")
    private String hypeManagerId;

    @Value("${opexy.roles.hype-events}")
    private String hypeEventsId;

    public AuctionCommand(EventManager eventManager, AuctionManager auctionManager) {
        this.eventManager = eventManager;
        this.auctionManager = auctionManager;
    }

    @Override
    public List<SlashCommandData> getCommandDataList() {
        return List.of(Commands.slash("auction", "بدء المزاد الأعمى (Blind Auction)")
                .addOptions(new OptionData(OptionType.STRING, "prize", "اسم الجائزة (مثلاً: 100k opex أو رتبة مميزة)", true)));
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("auction")) return;

        String prize = event.getOption("prize").getAsString();

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

        auctionManager.startAuction(prize);

        String body = "تم بدء مزاد على **" + prize + "**! 📦\n\n**القوانين:**\n• المزايدة تبدأ بـ 10 opex.\n• المزايدة الأعلى تفوز بالمحتوى.\n• قد يحتوي الصندوق على مفاجآت!";

        event.reply(new net.dv8tion.jda.api.utils.messages.MessageCreateBuilder()
                .setComponents(com.integrafty.opexy.utils.EmbedUtil.containerBranded("AUCTION", "🏆 المزاد الأعمى — Blind Auction", body, com.integrafty.opexy.utils.EmbedUtil.BANNER_MAIN,
                        net.dv8tion.jda.api.components.actionrow.ActionRow.of(
                                net.dv8tion.jda.api.components.buttons.Button.primary("bid_10", "+10"),
                                net.dv8tion.jda.api.components.buttons.Button.primary("bid_50", "+50"),
                                net.dv8tion.jda.api.components.buttons.Button.primary("bid_100", "+100"),
                                net.dv8tion.jda.api.components.buttons.Button.success("bid_custom", "سعر مخصص ✏️")
                        )))
                .useComponentsV2(true).build())
                .useComponentsV2(true).queue();
    }
}
