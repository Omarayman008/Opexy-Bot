package com.integrafty.opexy.service.event;

import com.integrafty.opexy.utils.EmbedUtil;
import com.integrafty.opexy.service.EconomyService;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ScavengerHuntManager extends ListenerAdapter {

    private final EventManager eventManager;
    private final AchievementService achievementService;
    private final EconomyService economyService;
    private final Random random = new Random();

    private String activeCode = null;
    private long reward = 5000;

    public String startHunt(long rewardAmount) {
        this.activeCode = "OP-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        this.reward = rewardAmount;
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
            activeCode = null; // One winner only
            eventManager.endGroupEvent();
            
            economyService.addBalance(event.getAuthor().getId(), event.getGuild().getId(), reward);
            
            event.getChannel().sendMessage(new net.dv8tion.jda.api.utils.messages.MessageCreateBuilder()
                    .setComponents(EmbedUtil.containerBranded("EVENT", "🎉 فائز بفعالية الصيد!", 
                            "مبروك <@" + event.getAuthor().getId() + ">! لقد وجدت الكود الصحيح وفزت بـ **" + reward + " opex**!", 
                            EmbedUtil.BANNER_MAIN))
                    .useComponentsV2(true).build())
                    .useComponentsV2(true).queue();
            
            // Stats
            achievementService.updateStats(event.getAuthor().getIdLong(), event.getGuild(), s -> {});
        }
    }
}
