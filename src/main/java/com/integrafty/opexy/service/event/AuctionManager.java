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

        // Update Embed
        EmbedBuilder embed = new EmbedBuilder(event.getMessage().getEmbeds().get(0))
                .clearFields()
                .addField("المزايد الحالي", event.getUser().getAsMention(), true)
                .addField("أعلى سعر", newTotal + " opex", true);

        event.editMessageEmbeds(embed.build()).queue();
    }

    private void resetTimer(ButtonInteractionEvent event) {
        if (endTask != null) endTask.cancel(false);
        
        endTask = scheduler.schedule(() -> {
            finishAuction(event);
        }, 30, TimeUnit.SECONDS);
    }

    private void finishAuction(ButtonInteractionEvent event) {
        eventManager.endGroupEvent();
        
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("🏁 انتهى المزاد!")
                .setColor(Color.GREEN)
                .setDescription("الفائز هو <@" + highestBidderId + "> بسعر **" + currentHighestBid + " opex**!")
                .addField("الجائزة", currentPrize, false)
                .setFooter("مبروك للفائز!");

        event.getChannel().sendMessageEmbeds(embed.build()).useComponentsV2(true).queue();
        
        // Finalize achievements
        achievementService.updateStats(highestBidderId, event.getGuild(), stats -> {
            stats.setSuccessBids(stats.getSuccessBids() + 1);
        });
        
        // Reset state
        currentHighestBid = 0;
        highestBidderId = 0;
    }
}
