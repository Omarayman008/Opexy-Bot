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
    private final Map<Long, net.dv8tion.jda.api.interactions.InteractionHook> userHooks = new HashMap<>();
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
            Map.entry("W", "<:Oak_Planks:1500856699651756144>"), // Wood/Planks
            Map.entry("S", "<:stick:1500856140882251877>"), // Stick
            Map.entry("I", "<:MinecraftIronIngot:1500855306953949237>"), // Iron Ingot
            Map.entry("G", "<:goldingot:1500855404635361301>"), // Gold Ingot
            Map.entry("D", "<:dimoand:1500855459811299530>"), // Diamond
            Map.entry("P", "<:MinecraftSugarCaneItemHDPngDownl:1500856346881429584>"), // Sugar Cane (Paper)
            Map.entry("B", "<:coblestone_block:1500856755020759102>"), // Cobblestone
            Map.entry("C", "<:obsadian:1500856426388521097>"), // Obsidian/Coal
            Map.entry("E", "🔲"), // Empty
            Map.entry("R", "<:redstone:1500855636454539315>"), // Redstone
            Map.entry("L", "<:String:1500863278484688906>"), // String
            Map.entry("F", "<:MinecraftGunpowderpng:1500855744470319146>"), // Gunpowder
            Map.entry("Q", "<:Quartz:1500863495036600513>"), // Quartz
            Map.entry("O", "<:obsadian:1500856426388521097>"), // Obsidian
            Map.entry("T", "<:torch:1500855870840635594>"), // Torch
            Map.entry("H", "<:EnchantedBookMinecraftPlugin6D:1500855968169328711>"), // Book
            Map.entry("A", "<:Red_Apple:1500863813019242546>"), // Apple
            Map.entry("N", "<:Ender_Eye:1500863572694143078>"), // Ender Eye
            Map.entry("K", "<:Nether_Star:1500863654051188921>"), // Nether Star
            Map.entry("U", "<:Leather:1500863416661971126>"), // Leather
            Map.entry("Z", "<:sand:1500856844296654998>"), // Sand
            Map.entry("V", "<:Glass_Botlle:1500863727724134521>"), // Glass Bottle
            Map.entry("M", "<:emrald:1500855555009286247>"), // Emerald
            Map.entry("Y", "<:Hay_Bale:1500857647132315659>")  // Hay Bale
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
            new Recipe(new String[][]{{"W", "W", "E"}, {"W", "W", "E"}, {"E", "E", "E"}}, List.of("ورك بينش", "طاولة صنع", "crafting table", "workbench", "طاوله صنع", "طاولة الصناعة", "كرفتنق تيبل", "كرافتنق تيبل"), "طاولة صنع", Difficulty.EASY),
            new Recipe(new String[][]{{"S", "E", "E"}, {"S", "E", "E"}, {"E", "E", "E"}}, List.of("عصا", "stick", "عصاي", "العصا", "ستيك"), "عصا", Difficulty.EASY),
            new Recipe(new String[][]{{"W", "W", "W"}, {"W", "W", "W"}, {"W", "W", "W"}}, List.of("بلوك خشب", "wood block", "خشب", "الخشب", "بلوك الخشب", "planks", "wood"), "بلوك خشب", Difficulty.EASY),
            new Recipe(new String[][]{{"C", "E", "E"}, {"S", "E", "E"}, {"E", "E", "E"}}, List.of("شمعة", "torch", "شعلة", "شمعه", "شعلة نار", "تورتش"), "شمعة", Difficulty.EASY),
            new Recipe(new String[][]{{"W", "E", "W"}, {"W", "W", "W"}, {"W", "E", "W"}}, List.of("سلم", "ladder", "سلم خشب", "السلم", "لادر"), "سلم خشب", Difficulty.EASY),
            new Recipe(new String[][]{{"W", "W", "E"}, {"W", "W", "E"}, {"W", "W", "E"}}, List.of("باب", "door", "باب خشب", "الباب", "دور"), "باب خشب", Difficulty.EASY),
            new Recipe(new String[][]{{"W", "W", "W"}, {"E", "E", "E"}, {"E", "E", "E"}}, List.of("سلاب", "slab", "بلاطة", "بلاطة خشب", "بلاطه"), "بلاطة خشب", Difficulty.EASY),
            
            // MEDIUM
            new Recipe(new String[][]{{"E", "D", "E"}, {"E", "D", "E"}, {"E", "S", "E"}}, List.of("سيف", "sword", "سيف دايموند", "سيف الدايموند", "السيف", "دايموند سورد"), "سيف دايموند", Difficulty.MEDIUM),
            new Recipe(new String[][]{{"D", "D", "D"}, {"E", "S", "E"}, {"E", "S", "E"}}, List.of("بيكاكس", "pickaxe", "فأس", "بيكاكس دايموند", "بيكاكس الدايموند", "الفأس", "دايموند بيكاكس"), "بيكاكس دايموند", Difficulty.MEDIUM),
            new Recipe(new String[][]{{"W", "W", "W"}, {"W", "E", "W"}, {"W", "W", "W"}}, List.of("صندوق", "chest", "تشيست", "الصندوق", "صندوق خشب"), "صندوق", Difficulty.MEDIUM),
            new Recipe(new String[][]{{"E", "S", "L"}, {"S", "E", "L"}, {"E", "S", "L"}}, List.of("قوس", "bow", "سهم", "القوس", "سهم وقوس", "بوو"), "قوس", Difficulty.MEDIUM),
            new Recipe(new String[][]{{"S", "S", "S"}, {"S", "L", "S"}, {"S", "S", "S"}}, List.of("صنارة", "fishing rod", "صنارة صيد", "الصنارة", "صناره", "فيشنق رود"), "صنارة صيد", Difficulty.MEDIUM),
            new Recipe(new String[][]{{"I", "I", "I"}, {"I", "E", "I"}, {"I", "I", "I"}}, List.of("خوذة", "helmet", "خوذة حديد", "خوذه", "الخوذة", "هيلت"), "خوذة حديد", Difficulty.MEDIUM),
            new Recipe(new String[][]{{"E", "R", "E"}, {"R", "I", "R"}, {"E", "R", "E"}}, List.of("بوصلة", "compass", "البوصلة", "بوصله", "كمباس"), "بوصلة", Difficulty.MEDIUM),
            new Recipe(new String[][]{{"E", "G", "E"}, {"G", "R", "G"}, {"E", "G", "E"}}, List.of("ساعة", "clock", "ساعه", "الساعة", "كلوك"), "ساعة", Difficulty.MEDIUM),
            
            // HARD
            new Recipe(new String[][]{{"I", "I", "I"}, {"I", "S", "I"}, {"E", "S", "E"}}, List.of("بيكاكس حديد", "iron pickaxe", "بيكاكس الحديد", "ايرون بيكاكس"), "بيكاكس حديد", Difficulty.HARD),
            new Recipe(new String[][]{{"G", "G", "G"}, {"E", "S", "E"}, {"E", "S", "E"}}, List.of("بيكاكس ذهب", "gold pickaxe", "بيكاكس الذهب", "قولد بيكاكس"), "بيكاكس ذهب", Difficulty.HARD),
            new Recipe(new String[][]{{"P", "P", "P"}, {"P", "E", "P"}, {"P", "P", "P"}}, List.of("خريطة", "map", "ماب", "خريطة فارغة", "الخريطة"), "خريطة فارغة", Difficulty.HARD),
            new Recipe(new String[][]{{"B", "B", "B"}, {"B", "E", "B"}, {"B", "B", "B"}}, List.of("فرن", "furnace", "الفرن", "فرنيس"), "فرن حجري", Difficulty.HARD),
            new Recipe(new String[][]{{"E", "H", "E"}, {"D", "O", "D"}, {"O", "O", "O"}}, List.of("طاولة تطوير", "enchantment table", "تطوير", "طاوله تطوير", "طاولة التطوير", "انشانتمنت تيبل"), "طاولة تطوير", Difficulty.HARD),
            new Recipe(new String[][]{{"I", "I", "I"}, {"E", "I", "E"}, {"I", "I", "I"}}, List.of("سندان", "anvil", "السندان", "انفيل"), "سندان", Difficulty.HARD),
            new Recipe(new String[][]{{"B", "B", "B"}, {"B", "R", "B"}, {"B", "B", "B"}}, List.of("نوت بلوك", "note block", "موسيقى", "النوت بلوك"), "نوت بلوك", Difficulty.HARD),
            new Recipe(new String[][]{{"I", "I", "I"}, {"I", "I", "I"}, {"E", "E", "E"}}, List.of("درع", "chestplate", "درع حديد", "الدرع", "درع الحديد", "تشيست بليت"), "درع حديد", Difficulty.HARD),
            new Recipe(new String[][]{{"G", "G", "G"}, {"G", "A", "G"}, {"G", "G", "G"}}, List.of("تفاحة ذهبية", "golden apple", "تفاحة ذهب", "قولدن ابل"), "تفاحة ذهبية", Difficulty.HARD),
            new Recipe(new String[][]{{"O", "O", "O"}, {"O", "N", "O"}, {"O", "O", "O"}}, List.of("صندوق اندر", "ender chest", "اندر تشيست"), "صندوق اندر", Difficulty.HARD),
            new Recipe(new String[][]{{"G", "G", "G"}, {"G", "K", "G"}, {"O", "O", "O"}}, List.of("بيكون", "beacon", "منارة", "بيكن"), "بيكون", Difficulty.HARD),
            new Recipe(new String[][]{{"F", "Z", "F"}, {"Z", "F", "Z"}, {"F", "Z", "F"}}, List.of("تي ان تي", "tnt", "متفجرات"), "TNT", Difficulty.HARD),
            new Recipe(new String[][]{{"F", "E", "E"}, {"S", "E", "E"}, {"J", "E", "E"}}, List.of("سهم", "arrow", "سهام"), "سهم", Difficulty.HARD),

            // TOOLS (DIAMOND)
            new Recipe(new String[][]{{"D", "D", "E"}, {"D", "S", "E"}, {"E", "S", "E"}}, List.of("فأس دايموند", "diamond axe", "فأس", "الفأس"), "فأس دايموند", Difficulty.MEDIUM),
            new Recipe(new String[][]{{"E", "D", "E"}, {"E", "S", "E"}, {"E", "S", "E"}}, List.of("مجرفة دايموند", "diamond shovel", "مجرفة", "مجرفه"), "مجرفة دايموند", Difficulty.EASY),
            new Recipe(new String[][]{{"D", "D", "E"}, {"E", "S", "E"}, {"E", "S", "E"}}, List.of("فأس زراعة", "diamond hoe", "محراث"), "فأس زراعة دايموند", Difficulty.MEDIUM),

            // ARMOR (DIAMOND)
            new Recipe(new String[][]{{"D", "E", "D"}, {"D", "E", "D"}, {"E", "E", "E"}}, List.of("حذاء دايموند", "diamond boots", "بوت", "حذاء"), "حذاء دايموند", Difficulty.EASY),
            new Recipe(new String[][]{{"D", "D", "D"}, {"D", "E", "D"}, {"D", "E", "D"}}, List.of("سروال دايموند", "diamond leggings", "بنطلون", "سروال"), "سروال دايموند", Difficulty.HARD),
            
            // UTILITY
            new Recipe(new String[][]{{"I", "E", "E"}, {"E", "I", "E"}, {"E", "E", "E"}}, List.of("مقص", "shears", "المقص"), "مقص", Difficulty.EASY),
            new Recipe(new String[][]{{"I", "E", "I"}, {"E", "I", "E"}, {"E", "E", "E"}}, List.of("سطل", "bucket", "سطل حديد", "جردل"), "سطل حديد", Difficulty.MEDIUM),
            new Recipe(new String[][]{{"W", "E", "W"}, {"E", "W", "E"}, {"E", "E", "E"}}, List.of("وعاء", "bowl", "صحن"), "وعاء خشبي", Difficulty.EASY),
            new Recipe(new String[][]{{"P", "P", "P"}, {"E", "E", "E"}, {"E", "E", "E"}}, List.of("ورق", "paper", "ورقة"), "ورق", Difficulty.EASY),
            new Recipe(new String[][]{{"K", "K", "K"}, {"E", "E", "E"}, {"E", "E", "E"}}, List.of("خبز", "bread", "الخبز"), "خبز", Difficulty.EASY),
            new Recipe(new String[][]{{"S", "S", "S"}, {"S", "U", "S"}, {"S", "S", "S"}}, List.of("لوحة", "painting", "لوحه"), "لوحة فنية", Difficulty.MEDIUM),
            new Recipe(new String[][]{{"Q", "Q", "Q"}, {"W", "W", "W"}, {"E", "E", "E"}}, List.of("سرير", "bed", "السرير"), "سرير", Difficulty.MEDIUM),
            new Recipe(new String[][]{{"W", "S", "W"}, {"W", "S", "W"}, {"E", "E", "E"}}, List.of("سياج", "fence", "سور"), "سياج خشبي", Difficulty.MEDIUM),
            new Recipe(new String[][]{{"S", "W", "S"}, {"S", "W", "S"}, {"E", "E", "E"}}, List.of("بوابة سياج", "fence gate", "بوابة"), "بوابة سياج", Difficulty.MEDIUM),
            new Recipe(new String[][]{{"B", "B", "B"}, {"B", "L", "B"}, {"B", "R", "B"}}, List.of("ديسبنسر", "dispenser", "موزع"), "ديسبنسر", Difficulty.HARD),
            new Recipe(new String[][]{{"G", "G", "G"}, {"Q", "Q", "Q"}, {"W", "W", "W"}}, List.of("حساس ضوء", "daylight sensor"), "حساس ضوء الشمس", Difficulty.HARD),
            new Recipe(new String[][]{{"I", "I", "I"}, {"I", "I", "I"}, {"I", "I", "I"}}, List.of("بلوك حديد", "iron block"), "بلوك حديد", Difficulty.MEDIUM),
            new Recipe(new String[][]{{"G", "G", "G"}, {"G", "G", "G"}, {"G", "G", "G"}}, List.of("بلوك ذهب", "gold block"), "بلوك ذهب", Difficulty.MEDIUM),
            new Recipe(new String[][]{{"D", "D", "D"}, {"D", "D", "D"}, {"D", "D", "D"}}, List.of("بلوك دايموند", "diamond block"), "بلوك دايموند", Difficulty.HARD),
            new Recipe(new String[][]{{"M", "M", "M"}, {"M", "M", "M"}, {"M", "M", "M"}}, List.of("بلوك زمرد", "emerald block"), "بلوك زمرد", Difficulty.HARD)
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
        sb.append("**      1      2      3**\n");
        for (int i = 0; i < 3; i++) {
            sb.append("**").append(i + 1).append("**  ");
            for (int j = 0; j < 3; j++) {
                sb.append(ITEMS.get(recipe.grid[i][j])).append(" ");
            }
            sb.append("\n");
        }

        // LOGGING SYSTEM
        String logDetails = String.format("### 🛠️ فعالية الصناعة: بدء (فردية)\n▫️ **اللاعب:** %s\n▫️ **الصعوبة:** %s\n▫️ **الجائزة:** %d opex", 
                organizer.getAsMention(), difficulty.displayName, difficulty.reward);
        logManager.logEmbed(guild, LogManager.LOG_GAMES, 
                EmbedUtil.createOldLogEmbed("craft", logDetails, organizer, null, null, EmbedUtil.INFO));

        return sb.toString();
    }

    public void initTimer(long userId, Difficulty difficulty, net.dv8tion.jda.api.interactions.callbacks.IReplyCallback event, String grid) {
        final int[] timeLeft = {difficulty.seconds};
        userHooks.put(userId, event.getHook());
        
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
        userHooks.remove(userId);
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
            net.dv8tion.jda.api.interactions.InteractionHook hook = userHooks.get(userId);
            stopTimer(userId);
            userActiveRecipes.remove(userId);
            userRewards.remove(userId);

            economyService.addBalance(event.getAuthor().getId(), event.getGuild().getId(), (int) reward);
            achievementService.updateStats(userId, event.getGuild(), s -> s.setCraftWins(s.getCraftWins() + 1));

            String successMsg = String.format("🎉 **كفوو!** إجابة صحيحة يا %s!\nتم صنع: **%s** بنجاح.\n💰 حصلت على **%d opex**", 
                    event.getAuthor().getAsMention(), itemName, reward);

            if (hook != null) {
                hook.editOriginal(new net.dv8tion.jda.api.utils.messages.MessageEditBuilder()
                        .setComponents(EmbedUtil.success("CRAFTING SUCCESS", "`=---------------- SUCCESS ----------------=`\n\n" + successMsg))
                        .useComponentsV2(true)
                        .build()).queue();
                event.getMessage().delete().queue(null, e -> {});
            } else {
                event.getChannel().sendMessage(new MessageCreateBuilder()
                        .setComponents(EmbedUtil.success("CRAFTING MASTER", successMsg))
                        .useComponentsV2(true)
                        .build()).queue();
            }

            // LOG WIN
            String logWin = String.format("### 🏆 فعالية الصناعة: فوز (فردية)\n▫️ **الفائز:** <@%s>\n▫️ **الصعوبة:** %s\n▫️ **الشيء:** %s", 
                    event.getAuthor().getId(), activeRecipe.difficulty.displayName, itemName);
            logManager.logEmbed(event.getGuild(), LogManager.LOG_GAMES, 
                    EmbedUtil.createOldLogEmbed("craft_win", logWin, event.getMember(), null, null, EmbedUtil.SUCCESS));
        }
    }
}
