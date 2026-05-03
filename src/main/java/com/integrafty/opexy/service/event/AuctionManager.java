package com.integrafty.opexy.service.event;

import com.integrafty.opexy.entity.UserStats;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Service
public class AuctionManager extends ListenerAdapter {

    private final AchievementService achievementService;
    private final EventManager eventManager;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public AuctionManager(AchievementService achievementService, EventManager eventManager) {
        this.achievementService = achievementService;
        this.eventManager = eventManager;
    }

    private long currentHighestBid = 0;
    private long highestBidderId = 0;
    private String currentPrize = "📦 صندوق عشوائي";
    private ScheduledFuture<?> endTask = null;
    private String activeMessageId = null;

    public void startAuction(String prize) {
        this.currentPrize = prize;
        this.currentHighestBid = 0;
        this.highestBidderId = 0;
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        if (!event.getComponentId().startsWith("bid_")) return;

        long userId = event.getUser().getIdLong();
        long bidAmount = 0;

        if (event.getComponentId().equals("bid_10")) bidAmount = 10;
        else if (event.getComponentId().equals("bid_50")) bidAmount = 50;
        else if (event.getComponentId().equals("bid_100")) bidAmount = 100;
        else if (event.getComponentId().equals("bid_custom")) {
            // Modal implementation would be better here, but for now we'll simulate
            event.reply("استخدم المزايدات السريعة حالياً!").setEphemeral(true).queue();
            return;
        }

        long newTotal = currentHighestBid + bidAmount;
        currentHighestBid = newTotal;
        highestBidderId = userId;
        activeMessageId = event.getMessageId();

        // Update stats for achievements
        achievementService.updateStats(userId, event.getGuild(), stats -> {
            if (newTotal > stats.getMaxBid()) stats.setMaxBid(newTotal);
        });

        // Reset timer
        resetTimer(event);

        // Update Message
        String body = "تم بدء مزاد على **جائزة غامضة**! 📦\n\n**القوانين:**\n• المزايدة تبدأ بـ 10 opex.\n• المزايدة الأعلى تفوز بالمحتوى.\n\n" +
                      "👤 المزايد الحالي: <@" + userId + ">\n💰 أعلى سعر: **" + newTotal + " opex**";

        event.editMessage(new net.dv8tion.jda.api.utils.messages.MessageEditBuilder()
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

    private void resetTimer(ButtonInteractionEvent event) {
        if (endTask != null) endTask.cancel(false);
        
        endTask = scheduler.schedule(() -> {
            finishAuction(event);
        }, 30, TimeUnit.SECONDS);
    }

    private void finishAuction(ButtonInteractionEvent event) {
        eventManager.endGroupEvent();
        
        String body = "الفائز هو <@" + highestBidderId + "> بسعر **" + currentHighestBid + " opex**!\n\n**الجائزة:** " + currentPrize + "\n\nمبروك للفائز!";
        
        event.getChannel().sendMessage(new net.dv8tion.jda.api.utils.messages.MessageCreateBuilder()
                .setComponents(com.integrafty.opexy.utils.EmbedUtil.containerBranded("AUCTION", "🏁 انتهى المزاد!", body, com.integrafty.opexy.utils.EmbedUtil.BANNER_MAIN))
                .useComponentsV2(true).build())
                .useComponentsV2(true).queue();
        
        // Finalize achievements
        achievementService.updateStats(highestBidderId, event.getGuild(), stats -> {
            stats.setSuccessBids(stats.getSuccessBids() + 1);
        });
        
        // Reset state
        currentHighestBid = 0;
        highestBidderId = 0;
    }
}
