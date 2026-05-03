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
        
        // Mafia - Eagle Eye
        body.append("**Eagle Eye**\n");
        body.append("*Hit One Mafia In Event*\n");
        body.append(String.format("Progress %s %d%%\n\n", getProgressBar(stats.getDetectiveReveals(), 1), calculatePercent(stats.getDetectiveReveals(), 1)));

        // Mafia - Guardian Angel
        body.append("**Guardian Angel**\n");
        body.append("*Save a citizen as Doctor*\n");
        body.append(String.format("Progress %s %d%%\n\n", getProgressBar(stats.getDoctorSaves(), 3), calculatePercent(stats.getDoctorSaves(), 3)));

        // Auction - Master
        body.append("**Auction Master**\n");
        body.append("*Win a high-stakes blind auction*\n");
        body.append(String.format("Progress %s %d%%\n\n", getProgressBar(stats.getSuccessBids(), 5), calculatePercent(stats.getSuccessBids(), 5)));

        // Minigames - Master Plumber
        body.append("**Master Plumber**\n");
        body.append("*Solve the complex pipe puzzle*\n");
        body.append(String.format("Progress %s %d%%\n\n", getProgressBar(stats.getPipeWins(), 4), calculatePercent(stats.getPipeWins(), 4)));

        // Minigames - Lightning Fast
        body.append("**Lightning Fast**\n");
        body.append("*Win the 7-second speed challenge*\n");
        body.append(String.format("Progress %s %d%%\n", getProgressBar(stats.getSpeedWins(), 1), calculatePercent(stats.getSpeedWins(), 1)));

        event.reply(new net.dv8tion.jda.api.utils.messages.MessageCreateBuilder()
                .setComponents(com.integrafty.opexy.utils.EmbedUtil.containerBranded("IDENTITY", "إنجازات أوبكس — " + event.getUser().getName(), body.toString(), com.integrafty.opexy.utils.EmbedUtil.BANNER_MAIN))
                .useComponentsV2(true).build())
                .useComponentsV2(true).queue();
    }

    private int calculatePercent(int current, int max) {
        return (int) Math.min(100, (((double) current / max) * 100));
    }

    private String getProgressBar(int current, int max) {
        int percent = (int) (((double) current / max) * 5); // 5 blocks total
        percent = Math.min(5, percent);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            if (i < percent) sb.append("▧");
            else sb.append("▢");
        }
        return sb.toString();
    }

}
