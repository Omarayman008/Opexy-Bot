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
    private final Map<Long, Difficulty> userDifficulty = new HashMap<>();
    private final Map<Long, String> userMentions = new HashMap<>();
    private final Map<Long, Long> userGuilds = new HashMap<>();
    private final Map<Long, java.util.concurrent.ScheduledFuture<?>> userTimers = new HashMap<>();
    private final java.util.concurrent.ScheduledExecutorService scheduler = java.util.concurrent.Executors.newScheduledThreadPool(4);

    public enum Difficulty {
        EASY(20, 30, "سهل"),
        MEDIUM(30, 20, "وسط"),
        HARD(40, 10, "صعب");

        public final int reward;
        public final int seconds;
        public final String displayName;
        Difficulty(int r, int s, String d) { this.reward = r; this.seconds = s; this.displayName = d; }
    }

    private static final Map<String, String> ITEMS = Map.ofEntries(
            Map.entry("W", "🪵"), // Wood/Planks
            Map.entry("S", "🥢"), // Stick
            Map.entry("I", "🪙"), // Iron Ingot
            Map.entry("G", "🧈"), // Gold Ingot
            Map.entry("D", "💎"), // Diamond
            Map.entry("P", "⬜"), // Paper
            Map.entry("B", "🧱"), // Brick/Stone
            Map.entry("C", "⬛"), // Coal/Obsidian
            Map.entry("E", "🔲"), // Empty
            Map.entry("R", "🔴"), // Redstone
            Map.entry("L", "🧵"), // Leather/String
            Map.entry("F", "🔥"), // Flint/Blaze Powder
            Map.entry("Q", "💎"), // Quartz (using Diamond for now or a white emoji)
            Map.entry("O", "🟣"), // Obsidian
            Map.entry("T", "🕯️"), // Torch/Fire
            Map.entry("H", "📖")  // Book
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
            new Recipe(new String[][]{{"C", "E", "E"}, {"S", "E", "E"}, {"E", "E", "E"}}, List.of("شمعة", "torch", "شعلة"), "شمعة", Difficulty.EASY),
            new Recipe(new String[][]{{"W", "E", "W"}, {"W", "W", "W"}, {"W", "E", "W"}}, List.of("سلم", "ladder", "سلم"), "سلم خشب", Difficulty.EASY),
            new Recipe(new String[][]{{"W", "W", "E"}, {"W", "W", "E"}, {"W", "W", "E"}}, List.of("باب", "door", "باب خشب"), "باب خشب", Difficulty.EASY),
            new Recipe(new String[][]{{"W", "W", "W"}, {"E", "E", "E"}, {"E", "E", "E"}}, List.of("سلاب", "slab", "بلاطة"), "بلاطة خشب", Difficulty.EASY),
            
            // MEDIUM
            new Recipe(new String[][]{{"E", "D", "E"}, {"E", "D", "E"}, {"E", "S", "E"}}, List.of("سيف", "sword", "سيف دايموند"), "سيف دايموند", Difficulty.MEDIUM),
            new Recipe(new String[][]{{"D", "D", "D"}, {"E", "S", "E"}, {"E", "S", "E"}}, List.of("بيكاكس", "pickaxe", "فأس"), "بيكاكس دايموند", Difficulty.MEDIUM),
            new Recipe(new String[][]{{"W", "W", "W"}, {"W", "E", "W"}, {"W", "W", "W"}}, List.of("صندوق", "chest", "تشيست"), "صندوق", Difficulty.MEDIUM),
            new Recipe(new String[][]{{"E", "S", "L"}, {"S", "E", "L"}, {"E", "S", "L"}}, List.of("قوس", "bow", "سهم"), "قوس", Difficulty.MEDIUM),
            new Recipe(new String[][]{{"S", "S", "S"}, {"S", "L", "S"}, {"S", "S", "S"}}, List.of("صنارة", "fishing rod", "صنارة صيد"), "صنارة صيد", Difficulty.MEDIUM),
            new Recipe(new String[][]{{"I", "I", "I"}, {"I", "E", "I"}, {"I", "I", "I"}}, List.of("خوذة", "helmet", "خوذة حديد"), "خوذة حديد", Difficulty.MEDIUM),
            new Recipe(new String[][]{{"E", "R", "E"}, {"R", "I", "R"}, {"E", "R", "E"}}, List.of("بوصلة", "compass", "بوصلة"), "بوصلة", Difficulty.MEDIUM),
            new Recipe(new String[][]{{"E", "G", "E"}, {"G", "R", "G"}, {"E", "G", "E"}}, List.of("ساعة", "clock", "ساعة"), "ساعة", Difficulty.MEDIUM),
            
            // HARD
            new Recipe(new String[][]{{"I", "I", "I"}, {"I", "S", "I"}, {"E", "S", "E"}}, List.of("بيكاكس حديد", "iron pickaxe"), "بيكاكس حديد", Difficulty.HARD),
            new Recipe(new String[][]{{"G", "G", "G"}, {"E", "S", "E"}, {"E", "S", "E"}}, List.of("بيكاكس ذهب", "gold pickaxe"), "بيكاكس ذهب", Difficulty.HARD),
            new Recipe(new String[][]{{"P", "P", "P"}, {"P", "E", "P"}, {"P", "P", "P"}}, List.of("خريطة", "map", "ماب"), "خريطة فارغة", Difficulty.HARD),
            new Recipe(new String[][]{{"B", "B", "B"}, {"B", "E", "B"}, {"B", "B", "B"}}, List.of("فرن", "furnace", "فرن"), "فرن حجري", Difficulty.HARD),
            new Recipe(new String[][]{{"E", "H", "E"}, {"D", "O", "D"}, {"O", "O", "O"}}, List.of("طاولة تطوير", "enchantment table", "تطوير"), "طاولة تطوير", Difficulty.HARD),
            new Recipe(new String[][]{{"I", "I", "I"}, {"E", "I", "E"}, {"I", "I", "I"}}, List.of("سندان", "anvil", "سندان"), "سندان", Difficulty.HARD),
            new Recipe(new String[][]{{"B", "B", "B"}, {"B", "R", "B"}, {"B", "B", "B"}}, List.of("نوت بلوك", "note block", "موسيقى"), "نوت بلوك", Difficulty.HARD),
            new Recipe(new String[][]{{"I", "I", "I"}, {"I", "I", "I"}, {"E", "E", "E"}}, List.of("درع", "chestplate", "درع حديد"), "درع حديد", Difficulty.HARD)
    );

    public String startCraft(long userId, Difficulty difficulty, Guild guild, Member organizer) {
        List<Recipe> possible = RECIPES.stream().filter(r -> r.difficulty == difficulty).toList();
        Recipe recipe = possible.get(new Random().nextInt(possible.size()));
        
        userActiveRecipes.put(userId, recipe);
        userRewards.put(userId, (long) difficulty.reward);
        userDifficulty.put(userId, difficulty);
        userMentions.put(userId, organizer.getAsMention());
        userGuilds.put(userId, guild.getIdLong());

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

    public void initTimer(long userId, Difficulty difficulty, net.dv8tion.jda.api.interactions.callbacks.IReplyCallback event, String grid) {
        final int[] timeLeft = {difficulty.seconds};
        
        java.util.concurrent.ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(() -> {
            try {
                timeLeft[0]--;
                
                if (timeLeft[0] <= 0) {
                    stopTimer(userId);
                    Recipe activeRecipe = userActiveRecipes.get(userId);
                    if (activeRecipe != null) {
                        userActiveRecipes.remove(userId);
                        userRewards.remove(userId);
                        
                        String failMsg = String.format("⏰ **انتهى الوقت!** لم تنجح في تخمين الشيء المطلوب.\n✅ الشيء الصحيح هو: **%s**\n❌ حظاً أوفر في المرة القادمة.", activeRecipe.displayName);
                        event.getHook().editOriginal(new net.dv8tion.jda.api.utils.messages.MessageEditBuilder()
                                .setComponents(EmbedUtil.error("CRAFTING TIMEOUT", "`=---------------- 00:00 ----------------=`\n\n" + failMsg))
                                .useComponentsV2(true)
                                .build()).queue();
                    }
                    return;
                }

                String body = getCraftBody(userMentions.get(userId), grid, difficulty.reward, timeLeft[0]);
                event.getHook().editOriginal(new net.dv8tion.jda.api.utils.messages.MessageEditBuilder()
                        .setComponents(EmbedUtil.containerBranded("CRAFTING", "🛠️ ماذا نصنع؟", body, EmbedUtil.BANNER_MAIN))
                        .useComponentsV2(true)
                        .build()).queue(null, e -> {
                            // Ignore transient errors
                        });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 1, 1, java.util.concurrent.TimeUnit.SECONDS);
        
        userTimers.put(userId, future);
    }

    private String getCraftBody(String mention, String grid, int reward, int seconds) {
        String timerFormat = String.format("`=----------------%02d:%02d----------------=`", 0, seconds);
        return timerFormat + "\n\n" +
               String.format("أمامك طاولة كرافتنق خاصة بك يا %s... خمن ما هو الشيء الذي يتم صنعه؟\n\n", mention) +
               grid + "\n" +
               "💰 الجائزة: **" + reward + " opex**\n\n" +
               "💡 اكتب الإجابة مباشرة في الشات!";
    }

    private void stopTimer(long userId) {
        if (userTimers.containsKey(userId)) {
            userTimers.get(userId).cancel(true);
            userTimers.remove(userId);
        }
        userDifficulty.remove(userId);
        userMentions.remove(userId);
        userGuilds.remove(userId);
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
            stopTimer(userId);
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
