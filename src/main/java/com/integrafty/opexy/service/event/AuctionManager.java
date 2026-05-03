package com.integrafty.opexy.service.event;

import com.integrafty.opexy.entity.UserStats;
import com.integrafty.opexy.service.EconomyService;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.modals.Modal;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
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
    private final EconomyService economyService;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public AuctionManager(AchievementService achievementService, EventManager eventManager, EconomyService economyService) {
        this.achievementService = achievementService;
        this.eventManager = eventManager;
        this.economyService = economyService;
    }

    private long currentHighestBid = 0;
    private long highestBidderId = 0;
    private String currentPrize = "📦 صندوق عشوائي";
    private int durationSeconds = 30;
    private long targetPrice = 0;
    private ScheduledFuture<?> endTask = null;
    private String activeMessageId = null;

    public void startAuction(String prize, int duration, long targetPrice) {
        this.currentPrize = prize;
        this.durationSeconds = duration > 0 ? duration : 30;
        this.targetPrice = targetPrice;
        this.currentHighestBid = 0;
        this.highestBidderId = 0;
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        if (!event.getComponentId().startsWith("bid_")) return;

        long userId = event.getUser().getIdLong();
        long bidAmount = 0;

        String id = event.getComponentId();
        if (id.equals("bid_10")) bidAmount = 10;
        else if (id.equals("bid_50")) bidAmount = 50;
        else if (id.equals("bid_100")) bidAmount = 100;
        else if (id.equals("bid_custom")) {
            TextInput bidInput = TextInput.create("bid_val", TextInputStyle.SHORT)
                    .setPlaceholder("أدخل المبلغ هنا (مثال: 500)")
                    .setRequired(true)
                    .build();

            event.replyModal(Modal.create("auction_custom_modal", "مزايدة مخصصة")
                    .addComponents(net.dv8tion.jda.api.components.label.Label.of("سعر المزايدة", bidInput))
                    .build()).queue();
            return;
        }

        processBid(event, userId, bidAmount);
    }

    private void processBid(net.dv8tion.jda.api.interactions.callbacks.IReplyCallback event, long userId, long bidAmount) {
        long newTotal = currentHighestBid + bidAmount;
        
        // 1. Check Money
        long balance = economyService.getBalance(String.valueOf(userId), event.getGuild().getId());
        if (balance < newTotal) {
            event.reply("❌ عذراً، رصيدك غير كافٍ للمزايدة! (رصيدك: " + balance + " opex)").setEphemeral(true).queue();
            return;
        }

        currentHighestBid = newTotal;
        highestBidderId = userId;

        // Update stats
        achievementService.updateStats(userId, event.getGuild(), stats -> {
            if (newTotal > stats.getMaxBid()) stats.setMaxBid(newTotal);
        });

        // 2. Check Target Price
        if (targetPrice > 0 && newTotal >= targetPrice) {
            if (event instanceof net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback mec) mec.deferEdit().queue();
            finishAuction(event);
            return;
        }

        resetTimer(event);

        // Update Message
        String body = "تم بدء مزاد على **جائزة غامضة**! 📦\n\n**القوانين:**\n• المزايدة تبدأ بـ 10 opex.\n• المزايدة الأعلى تفوز بالمحتوى.\n\n" +
                      "👤 المزايد الحالي: <@" + userId + ">\n💰 أعلى سعر: **" + newTotal + " opex**";

        net.dv8tion.jda.api.utils.messages.MessageEditBuilder reply = new net.dv8tion.jda.api.utils.messages.MessageEditBuilder()
                .setComponents(com.integrafty.opexy.utils.EmbedUtil.containerBranded("AUCTION", "🏆 المزاد الأعمى — Blind Auction", body, com.integrafty.opexy.utils.EmbedUtil.BANNER_MAIN,
                        net.dv8tion.jda.api.components.actionrow.ActionRow.of(
                                net.dv8tion.jda.api.components.buttons.Button.primary("bid_10", "+10"),
                                net.dv8tion.jda.api.components.buttons.Button.primary("bid_50", "+50"),
                                net.dv8tion.jda.api.components.buttons.Button.primary("bid_100", "+100"),
                                net.dv8tion.jda.api.components.buttons.Button.success("bid_custom", "سعر مخصص ✏️")
                        )))
                .useComponentsV2(true);

        if (event instanceof net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback mec) {
            mec.editMessage(reply.build()).useComponentsV2(true).queue();
        }
    }

    @Override
    public void onModalInteraction(net.dv8tion.jda.api.events.interaction.ModalInteractionEvent event) {
        if (!event.getModalId().equals("auction_custom_modal")) return;
        
        try {
            long bidAmount = Long.parseLong(event.getValue("bid_val").getAsString());
            if (bidAmount <= 0) {
                event.reply("❌ يجب أن يكون المبلغ أكبر من 0!").setEphemeral(true).queue();
                return;
            }
            processBid(event, event.getUser().getIdLong(), bidAmount);
        } catch (NumberFormatException e) {
            event.reply("❌ الرجاء إدخال رقم صحيح.").setEphemeral(true).queue();
        }
    }

    private void resetTimer(net.dv8tion.jda.api.interactions.callbacks.IReplyCallback event) {
        if (endTask != null) endTask.cancel(false);
        
        endTask = scheduler.schedule(() -> {
            finishAuction(event);
        }, durationSeconds, TimeUnit.SECONDS);
    }

    private void finishAuction(net.dv8tion.jda.api.interactions.callbacks.IReplyCallback event) {
        eventManager.endGroupEvent();
        
        if (highestBidderId != 0) {
            economyService.subtractBalance(String.valueOf(highestBidderId), event.getGuild().getId(), currentHighestBid);
            
            achievementService.updateStats(highestBidderId, event.getGuild(), stats -> {
                stats.setSuccessBids(stats.getSuccessBids() + 1);
            });
        }

        String body = highestBidderId != 0 ? 
            "الفائز هو <@" + highestBidderId + "> بسعر **" + currentHighestBid + " opex**!\n\n**الجائزة:** " + currentPrize + "\n\nمبروك للفائز!" :
            "انتهى المزاد دون وجود أي مزايدات.";
        
        event.getMessageChannel().sendMessage(new net.dv8tion.jda.api.utils.messages.MessageCreateBuilder()
                .setComponents(com.integrafty.opexy.utils.EmbedUtil.containerBranded("AUCTION", "🏁 انتهى المزاد!", body, com.integrafty.opexy.utils.EmbedUtil.BANNER_MAIN))
                .useComponentsV2(true).build())
                .useComponentsV2(true).queue();
        
        // Reset state
        currentHighestBid = 0;
        highestBidderId = 0;
        if (endTask != null) endTask.cancel(false);
    }
}
