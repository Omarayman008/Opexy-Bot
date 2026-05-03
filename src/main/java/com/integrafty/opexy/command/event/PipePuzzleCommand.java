package com.integrafty.opexy.command.event;

import com.integrafty.opexy.command.base.MultiSlashCommand;
import com.integrafty.opexy.service.event.AchievementService;
import com.integrafty.opexy.service.event.EventManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.util.List;
import java.util.Random;

@Component
public class PipePuzzleCommand implements MultiSlashCommand {

    private final AchievementService achievementService;
    private final EventManager eventManager;
    private final Random random = new Random();

    @Value("${opexy.roles.hype-manager}")
    private String hypeManagerId;

    @Value("${opexy.roles.hype-events}")
    private String hypeEventsId;

    public PipePuzzleCommand(AchievementService achievementService, EventManager eventManager) {
        this.achievementService = achievementService;
        this.eventManager = eventManager;
    }

    @Override
    public List<SlashCommandData> getCommandDataList() {
        return List.of(Commands.slash("pipes", "بدء لغز أنابيب التوصيل (Connect the Pipes)")
                .addOptions(new OptionData(OptionType.STRING, "difficulty", "الصعوبة", true)
                        .addChoice("سهل (3x3)", "easy")
                        .addChoice("متوسط (4x4)", "medium")
                        .addChoice("صعب (5x5)", "hard")));
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("pipes")) return;

        // Check permissions
        boolean hasRole = event.getMember().getRoles().stream()
                .anyMatch(r -> r.getId().equals(hypeManagerId) || r.getId().equals(hypeEventsId));
        
        if (!hasRole) {
            event.reply("❌ عذراً، هذا الأمر مخصص لمشرفي الفعاليات فقط.").setEphemeral(true).queue();
            return;
        }

        String difficulty = event.getOption("difficulty") != null ? event.getOption("difficulty").getAsString() : "easy";
        int size = difficulty.equals("easy") ? 3 : difficulty.equals("medium") ? 4 : 5;
        
        String[][] grid = new String[size][size];
        String[] pieces = {"═", "║", "╔", "╗", "╚", "╝"};
        
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                grid[i][j] = pieces[random.nextInt(pieces.length)];
            }
        }

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("🔧 لغز الأنابيب — صعوبة: " + difficulty)
                .setColor(Color.CYAN)
                .setDescription("قم بتوصيل الأنابيب للوصول إلى المخرج!\n\n" + renderGrid(grid))
                .setFooter("اضغط على الأزرار لتدوير القطع (قريباً)");

        event.replyEmbeds(embed.build()).queue();
    }

    private String renderGrid(String[][] grid) {
        StringBuilder sb = new StringBuilder("```\n");
        for (String[] row : grid) {
            for (String cell : row) {
                sb.append(cell).append(" ");
            }
            sb.append("\n");
        }
        sb.append("```");
        return sb.toString();
    }
}
