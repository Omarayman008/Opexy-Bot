package com.integrafty.opexy.command.event;

import com.integrafty.opexy.command.base.MultiSlashCommand;
import com.integrafty.opexy.entity.UserStats;
import com.integrafty.opexy.service.event.AchievementService;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.springframework.stereotype.Component;

import java.util.List;
import java.awt.Color;

@Component
public class AchievementCommand implements MultiSlashCommand {

    private final AchievementService achievementService;

    public AchievementCommand(AchievementService achievementService) {
        this.achievementService = achievementService;
    }

    @Override
    public List<SlashCommandData> getCommandDataList() {
        return List.of(Commands.slash("achive", "عرض إنجازاتك وتقدمك في الفعاليات"));
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("achive")) return;
        
        UserStats stats = achievementService.getStats(event.getUser().getIdLong());
        
        StringBuilder body = new StringBuilder();
        body.append("✨ استعرض تقدمك وإحصائياتك في عالم أوبكس التفاعلي.\nكلما زاد نشاطك، اقتربت من الرتب النادرة!\n\n");
        
        body.append("### 🔨 المزاد الأعمى (Auction)\n");
        body.append(String.format("```\n• فوز بالمزاد: %d/5 %s\n• مزايدة فاشلة: %d/5 %s\n• أعلى مبلغ: %d opex\n```\n", 
                        stats.getSuccessBids(), getProgressBar(stats.getSuccessBids(), 5),
                        stats.getFailedBids(), getProgressBar(stats.getFailedBids(), 5),
                        stats.getMaxBid()));

        body.append("### 🕵️ عالم المافيا (Mafia)\n");
        body.append(String.format("```\n• فوز المافيا: %d/6 %s\n• مرات المواطن: %d/8 %s\n• أصوات ضدك: %d/15 %s\n• كشف المافيا: %d/1 %s\n• حماية ناجحة: %d/3 %s\n```\n", 
                        stats.getMafiaWins(), getProgressBar(stats.getMafiaWins(), 6),
                        stats.getCitizenCount(), getProgressBar(stats.getCitizenCount(), 8),
                        stats.getVotesReceived(), getProgressBar(stats.getVotesReceived(), 15),
                        stats.getDetectiveReveals(), getProgressBar(stats.getDetectiveReveals(), 1),
                        stats.getDoctorSaves(), getProgressBar(stats.getDoctorSaves(), 3)));

        body.append("### 🎮 الألعاب المصغرة\n");
        body.append(String.format("```\n• سباك الأنابيب: %d/4 %s\n• الـ 7 ثواني: %d/1 %s\n```", 
                        stats.getPipeWins(), getProgressBar(stats.getPipeWins(), 4),
                        stats.getSpeedWins(), getProgressBar(stats.getSpeedWins(), 1)));

        event.reply(new net.dv8tion.jda.api.utils.messages.MessageCreateBuilder()
                .setComponents(com.integrafty.opexy.utils.EmbedUtil.containerBranded("IDENTITY", "إنجازات أوبكس — " + event.getUser().getName(), body.toString(), com.integrafty.opexy.utils.EmbedUtil.BANNER_MAIN))
                .useComponentsV2(true).build())
                .useComponentsV2(true).queue();
    }

    private String getProgressBar(int current, int max) {
        int percent = (int) (((double) current / max) * 10);
        percent = Math.min(10, percent);
        StringBuilder sb = new StringBuilder("`[");
        for (int i = 0; i < 10; i++) {
            if (i < percent) sb.append("▰");
            else sb.append("▱");
        }
        sb.append("]`");
        return sb.toString();
    }

}
