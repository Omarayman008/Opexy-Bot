package com.integrafty.opexy.service.event;

import com.integrafty.opexy.service.EconomyService;
import com.integrafty.opexy.service.LogManager;
import com.integrafty.opexy.utils.EmbedUtil;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class CraftManager extends ListenerAdapter {

    private final EventManager eventManager;
    private final AchievementService achievementService;
    private final EconomyService economyService;
    private final LogManager logManager;

    private Recipe activeRecipe = null;
    private long reward = 5000;

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
        final List<String> possibleNames; // Multiple names for same item (sword, سيف)
        final String displayName;
    }

    private static final List<Recipe> RECIPES = List.of(
            new Recipe(new String[][]{{"E", "D", "E"}, {"E", "D", "E"}, {"E", "S", "E"}}, List.of("سيف", "sword", "سيف دايموند"), "سيف دايموند"),
            new Recipe(new String[][]{{"D", "D", "D"}, {"E", "S", "E"}, {"E", "S", "E"}}, List.of("بيكاكس", "pickaxe", "فأس"), "بيكاكس دايموند"),
            new Recipe(new String[][]{{"W", "W", "W"}, {"W", "E", "W"}, {"W", "W", "W"}}, List.of("صندوق", "chest", "تشيست"), "صندوق"),
            new Recipe(new String[][]{{"B", "B", "B"}, {"B", "E", "B"}, {"B", "B", "B"}}, List.of("فرن", "furnace", "فرن"), "فرن حجري"),
            new Recipe(new String[][]{{"I", "I", "I"}, {"E", "I", "E"}, {"E", "I", "E"}}, List.of("درع", "chestplate", "درع حديد"), "درع حديدي"),
            new Recipe(new String[][]{{"E", "E", "E"}, {"S", "S", "S"}, {"S", "S", "S"}}, List.of("سلم", "ladder", "سلم"), "سلم خشب"),
            new Recipe(new String[][]{{"W", "W", "E"}, {"W", "S", "E"}, {"E", "S", "E"}}, List.of("اكس", "axe", "فأس"), "فأس خشبي"),
            new Recipe(new String[][]{{"P", "P", "P"}, {"P", "E", "P"}, {"P", "P", "P"}}, List.of("خريطة", "map", "ماب"), "خريطة فارغة")
    );

    public String startCraft(long rewardAmount, Guild guild, Member organizer) {
        this.reward = rewardAmount;
        this.activeRecipe = RECIPES.get(new Random().nextInt(RECIPES.size()));

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                sb.append(ITEMS.get(activeRecipe.grid[i][j])).append(" ");
            }
            sb.append("\n");
        }

        // LOGGING
        String logDetails = String.format("### 🛠️ فعالية الصناعة: بدء الفعالية\n▫️ **المنظم:** %s\n▫️ **الجائزة:** %d opex\n▫️ **الشيء المطلوب:** %s", 
                organizer.getAsMention(), rewardAmount, activeRecipe.displayName);
        logManager.logEmbed(guild, LogManager.LOG_GAMES, 
                EmbedUtil.createOldLogEmbed("craft", logDetails, organizer, null, null, EmbedUtil.INFO));

        return sb.toString();
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot() || activeRecipe == null) return;

        String content = event.getMessage().getContentRaw().trim().toLowerCase();
        if (activeRecipe.possibleNames.contains(content)) {
            String winnerId = event.getAuthor().getId();
            String itemName = activeRecipe.displayName;
            activeRecipe = null;
            eventManager.endGroupEvent();

            economyService.addBalance(winnerId, event.getGuild().getId(), (int) reward);
            achievementService.incrementGameWin(winnerId);

            event.getChannel().sendMessageEmbeds(EmbedUtil.success("CRAFTING MASTER", 
                    String.format("✅ كفو <@%s>! الإجابة صحيحة، الشيء هو **%s**.\n💰 ربحت **%d opex**!", winnerId, itemName, reward)).getEmbeds().get(0)).queue();

            // LOG WIN
            String logWin = String.format("### 🏆 فعالية الصناعة: فوز\n▫️ **الفائز:** <@%s>\n▫️ **الجائزة:** %d opex\n▫️ **الشيء:** %s", 
                    winnerId, reward, itemName);
            logManager.logEmbed(event.getGuild(), LogManager.LOG_GAMES, 
                    EmbedUtil.createOldLogEmbed("craft_win", logWin, event.getMember(), null, null, EmbedUtil.SUCCESS));
        }
    }
}
