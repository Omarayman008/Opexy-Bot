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
        
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("🏆 إنجازات أوبكس — " + event.getUser().getName())
                .setColor(new Color(0xFFD700)) // Gold
                .setThumbnail(event.getUser().getEffectiveAvatarUrl())
                .setDescription("هنا تظهر إحصائياتك وتقدمك نحو الحصول على رتب الإنجازات الحصرية.");

        // Auction Stats
        embed.addField("🔨 المزاد (Auction)", 
                String.format("• المزايدات الناجحة: %d/5 %s\n• المزايدات الفاشلة: %d/5 %s\n• أعلى مزايدة: %d", 
                        stats.getSuccessBids(), getProgressBar(stats.getSuccessBids(), 5),
                        stats.getFailedBids(), getProgressBar(stats.getFailedBids(), 5),
                        stats.getMaxBid()), true);

        // Mafia Stats
        embed.addField("🕵️ المافيا (Mafia)", 
                String.format("• فوز المافيا: %d/6 %s\n• جولات المواطن: %d/8 %s\n• أصوات ضدك: %d/15 %s\n• كشف المافيا: %d/1 %s\n• إنقاذ مواطنين: %d/3 %s", 
                        stats.getMafiaWins(), getProgressBar(stats.getMafiaWins(), 6),
                        stats.getCitizenCount(), getProgressBar(stats.getCitizenCount(), 8),
                        stats.getVotesReceived(), getProgressBar(stats.getVotesReceived(), 15),
                        stats.getDetectiveReveals(), getProgressBar(stats.getDetectiveReveals(), 1),
                        stats.getDoctorSaves(), getProgressBar(stats.getDoctorSaves(), 3)), true);

        // Minigames Stats
        embed.addField("🎮 ألعاب مصغرة", 
                String.format("• سباك الأنابيب: %d/4 %s\n• تحدي السرعة: %d/1 %s", 
                        stats.getPipeWins(), getProgressBar(stats.getPipeWins(), 4),
                        stats.getSpeedWins(), getProgressBar(stats.getSpeedWins(), 1)), true);

        embed.setFooter("Opexy Bot — نظام الإنجازات المتطور", event.getGuild().getIconUrl());
        
        event.replyEmbeds(embed.build()).queue();
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
