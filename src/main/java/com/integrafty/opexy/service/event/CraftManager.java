package com.integrafty.opexy.service.event;

import com.integrafty.opexy.service.EconomyService;
import com.integrafty.opexy.service.LogManager;
import com.integrafty.opexy.utils.EmbedUtil;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class CraftManager extends ListenerAdapter {

    private final AchievementService achievementService;
    private final EconomyService economyService;
    private final LogManager logManager;

    private final Map<Long, Recipe> userActiveRecipes = new HashMap<>();
    private final Map<Long, Long> userRewards = new HashMap<>();

    public enum Difficulty {
        EASY(20, "سهل"),
        MEDIUM(30, "وسط"),
        HARD(40, "صعب");

        public final int reward;
        public final String displayName;
        Difficulty(int r, String d) { this.reward = r; this.displayName = d; }
    }

    private static final Map<String, String> ITEMS = Map.of(
            "W", "🪵", // Wood
            "S", "🥢", // Stick
            "I", "🪙", // Iron
            "G", "🧈", // Gold
            "D", "💎", // Diamond
            "P", "⬜", // Paper
            "B", "🧱", // Brick/Stone
            "C", "⬛", // Coal/Obsidian
            "E", "🔲"  // Empty
    );

    @RequiredArgsConstructor
    private static class Recipe {
        final String[][] grid;
        final List<String> possibleNames;
        final String displayName;
        final Difficulty difficulty;
    }

    private static final List<Recipe> RECIPES = List.of(
            // EASY
            new Recipe(new String[][]{{"W", "W", "E"}, {"W", "W", "E"}, {"E", "E", "E"}}, List.of("ورك بينش", "طاولة صنع", "crafting table", "workbench"), "طاولة صنع", Difficulty.EASY),
            new Recipe(new String[][]{{"S", "E", "E"}, {"S", "E", "E"}, {"E", "E", "E"}}, List.of("عصا", "stick", "عصاي"), "عصا", Difficulty.EASY),
            new Recipe(new String[][]{{"W", "W", "W"}, {"W", "W", "W"}, {"W", "W", "W"}}, List.of("بلوك خشب", "wood block", "خشب"), "بلوك خشب", Difficulty.EASY),
            
            // MEDIUM
            new Recipe(new String[][]{{"E", "D", "E"}, {"E", "D", "E"}, {"E", "S", "E"}}, List.of("سيف", "sword", "سيف دايموند"), "سيف دايموند", Difficulty.MEDIUM),
            new Recipe(new String[][]{{"D", "D", "D"}, {"E", "S", "E"}, {"E", "S", "E"}}, List.of("بيكاكس", "pickaxe", "فأس"), "بيكاكس دايموند", Difficulty.MEDIUM),
            new Recipe(new String[][]{{"W", "W", "W"}, {"W", "E", "W"}, {"W", "W", "W"}}, List.of("صندوق", "chest", "تشيست"), "صندوق", Difficulty.MEDIUM),
            
            // HARD
            new Recipe(new String[][]{{"I", "I", "I"}, {"I", "S", "I"}, {"E", "S", "E"}}, List.of("بيكاكس حديد", "iron pickaxe"), "بيكاكس حديد", Difficulty.HARD),
            new Recipe(new String[][]{{"G", "G", "G"}, {"E", "S", "E"}, {"E", "S", "E"}}, List.of("بيكاكس ذهب", "gold pickaxe"), "بيكاكس ذهب", Difficulty.HARD),
            new Recipe(new String[][]{{"P", "P", "P"}, {"P", "E", "P"}, {"P", "P", "P"}}, List.of("خريطة", "map", "ماب"), "خريطة فارغة", Difficulty.HARD),
            new Recipe(new String[][]{{"B", "B", "B"}, {"B", "E", "B"}, {"B", "B", "B"}}, List.of("فرن", "furnace", "فرن"), "فرن حجري", Difficulty.HARD)
    );

    public String startCraft(long userId, Difficulty difficulty, Guild guild, Member organizer) {
        List<Recipe> possible = RECIPES.stream().filter(r -> r.difficulty == difficulty).toList();
        Recipe recipe = possible.get(new Random().nextInt(possible.size()));
        
        userActiveRecipes.put(userId, recipe);
        userRewards.put(userId, (long) difficulty.reward);

        StringBuilder sb = new StringBuilder();
        sb.append("```\n");
        sb.append("   1   2   3\n");
        for (int i = 0; i < 3; i++) {
            sb.append(i + 1).append(" ");
            for (int j = 0; j < 3; j++) {
                sb.append("[").append(ITEMS.get(recipe.grid[i][j])).append("]");
            }
            sb.append("\n");
        }
        sb.append("```");

        // LOGGING
        String logDetails = String.format("### 🛠️ فعالية الصناعة: بدء (فردية)\n▫️ **اللاعب:** %s\n▫️ **الصعوبة:** %s\n▫️ **الجائزة:** %d opex", 
                organizer.getAsMention(), difficulty.displayName, difficulty.reward);
        logManager.logEmbed(guild, LogManager.LOG_GAMES, 
                EmbedUtil.createOldLogEmbed("craft", logDetails, organizer, null, null, EmbedUtil.INFO));

        return sb.toString();
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        long userId = event.getAuthor().getIdLong();
        if (event.getAuthor().isBot() || !userActiveRecipes.containsKey(userId)) return;

        Recipe activeRecipe = userActiveRecipes.get(userId);
        long reward = userRewards.get(userId);

        String content = event.getMessage().getContentRaw().trim().toLowerCase();
        if (activeRecipe.possibleNames.contains(content)) {
            String itemName = activeRecipe.displayName;
            userActiveRecipes.remove(userId);
            userRewards.remove(userId);

            economyService.addBalance(event.getAuthor().getId(), event.getGuild().getId(), (int) reward);
            achievementService.updateStats(userId, event.getGuild(), s -> s.setCraftWins(s.getCraftWins() + 1));

            event.getChannel().sendMessage(new MessageCreateBuilder()
                    .setComponents(EmbedUtil.success("CRAFTING MASTER", 
                            String.format("✅ كفو <@%s>! الإجابة صحيحة، الشيء هو **%s**.\n💰 ربحت **%d opex**!", event.getAuthor().getId(), itemName, reward)))
                    .useComponentsV2(true)
                    .build()).queue();

            // LOG WIN
            String logWin = String.format("### 🏆 فعالية الصناعة: فوز (فردية)\n▫️ **الفائز:** <@%s>\n▫️ **الصعوبة:** %s\n▫️ **الشيء:** %s", 
                    event.getAuthor().getId(), activeRecipe.difficulty.displayName, itemName);
            logManager.logEmbed(event.getGuild(), LogManager.LOG_GAMES, 
                    EmbedUtil.createOldLogEmbed("craft_win", logWin, event.getMember(), null, null, EmbedUtil.SUCCESS));
        }
    }
}
