package com.integrafty.opexy.service.event;

import com.integrafty.opexy.utils.EmbedUtil;
import com.integrafty.opexy.service.EconomyService;
import com.integrafty.opexy.service.LogManager;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.stereotype.Service;

import net.dv8tion.jda.api.JDA;
import jakarta.annotation.PostConstruct;
import java.util.Collections;
import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ScavengerHuntManager extends ListenerAdapter {

    private final JDA jda;
    private final EventManager eventManager;
    private final AchievementService achievementService;
    private final EconomyService economyService;
    private final LogManager logManager;
    private final Random random = new Random();

    private String activeCode = null;
    private long reward = 5000;

    @PostConstruct
    public void init() {
        // Redundant - CommandManager registers all ListenerAdapter beans automatically
        // jda.addEventListener(this);
    }

    public String startHunt(long rewardAmount, net.dv8tion.jda.api.entities.Guild guild, net.dv8tion.jda.api.entities.Member organizer) {
        this.activeCode = "OP-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        this.reward = rewardAmount;
        
        // LOGGING
        String logDetails = String.format("### 🔍 فعالية الصيد: بدء الفعالية\n▫️ **المنظم:** %s\n▫️ **الجائزة:** %d opex\n▫️ **الكود:** ||%s||", 
                organizer.getAsMention(), rewardAmount, activeCode);
        logManager.logEmbed(guild, LogManager.LOG_GAMES, 
                EmbedUtil.createOldLogEmbed("hunt", logDetails, organizer, null, null, EmbedUtil.INFO));
                
        return activeCode;
    }


    public void stopHunt() {
        this.activeCode = null;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot() || activeCode == null) return;

        String content = event.getMessage().getContentRaw().trim().toUpperCase();
        if (content.equals(activeCode)) {
            String winnerId = event.getAuthor().getId();
            activeCode = null; // One winner only
            eventManager.endGroupEvent();
            
            economyService.addBalance(winnerId, event.getGuild().getId(), reward);
            
            event.getChannel().sendMessage(new net.dv8tion.jda.api.utils.messages.MessageCreateBuilder()
                    .setComponents(EmbedUtil.success("EVENT", "🎉 مبروك <@" + winnerId + ">! لقد وجدت الكود الصحيح وفزت بـ **" + reward + " opex**!"))
                    .useComponentsV2(true)
                    .build())
                    .useComponentsV2(true)
                    .queue();
            
            // LOGGING
            String logDetails = String.format("### 🔍 فعالية الصيد: انتهت الفعالية\n▫️ **الفائز:** <@%s>\n▫️ **الجائزة:** %d opex", 
                    winnerId, reward);
            logManager.logEmbed(event.getGuild(), LogManager.LOG_GAMES, 
                    EmbedUtil.createOldLogEmbed("hunt", logDetails, null, null, null, EmbedUtil.SUCCESS));

            // Stats
            achievementService.updateStats(event.getAuthor().getIdLong(), event.getGuild(), s -> {});
        }
    }
}
