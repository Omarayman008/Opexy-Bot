package com.integrafty.opexy.service.event;

import com.integrafty.opexy.service.EconomyService;
import com.integrafty.opexy.service.LogManager;
import com.integrafty.opexy.utils.EmbedUtil;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class BombManager extends ListenerAdapter {
    private final AchievementService achievementService;
    private final EconomyService economyService;
    private final LogManager logManager;

    public enum Difficulty {
        EASY(10, 15, "سهل"),
        MEDIUM(15, 10, "وسط"),
        HARD(20, 5, "صعب");

        public final int reward;
        public final int seconds;
        public final String displayName;
        Difficulty(int r, int s, String d) { this.reward = r; this.seconds = s; this.displayName = d; }
    }

    private final Map<Long, String> userCorrectWire = new HashMap<>();
    private final Map<Long, Long> userRewards = new HashMap<>();
    private final Map<Long, Long> userGuilds = new HashMap<>();
    private final Map<Long, Difficulty> userDifficulty = new HashMap<>();
    private final Map<Long, String> userHints = new HashMap<>();
    private final Map<Long, String> userMentions = new HashMap<>();
    private final Map<Long, java.util.concurrent.ScheduledFuture<?>> userTimers = new HashMap<>();
    private final java.util.concurrent.ScheduledExecutorService scheduler = java.util.concurrent.Executors.newScheduledThreadPool(4);

    private static final Map<String, String> WIRE_COLORS = Map.of(
            "red", "الأحمر",
            "blue", "الأزرق",
            "green", "الأخضر",
            "yellow", "الأصفر",
            "purple", "البنفسجي"
    );

    private static final Map<String, List<String>> HARD_HINTS = Map.of(
            "red", List.of(
                "دماء التنين المسفوكة على الثلج", "نواة النجم الأحمر في عمق الأرض", "لون حجر الردستون المشتعل بالنبض",
                "وميض الـ TNT قبل الانفجار الكبير", "لون تفاحة القوة المحرمة", "شروق شمس الجحيم خلف القلعة"
            ),
            "blue", List.of(
                "لون سماء النهاية قبل الغروب", "دموع الغاست المتجمدة في الجحيم", "بريق اللازورد في أعماق المحيطات",
                "لون الجليد المضغوط في القطب", "قوة شاردات الدايموند المسجونة", "لون مياه البحر العميقة تحت الجليد"
            ),
            "green", List.of(
                "عين الأندر مان الغاضبة تحت ضوء القمر", "لون الزمرد الذي يفتح أبواب القرى", "عشب الغابة الذي لم تطأه قدم",
                "سم الكريبر السائل في عروقه", "لون شجر الغابة العملاق", "بريق عيون السلايم في المستنقع"
            ),
            "yellow", List.of(
                "بريق الذهب الذي أعمى ملوك الجحيم", "لون مسحوق البليز المتطاير في القلعة", "شروق الشمس فوق رمال الصحراء",
                "لون الحنطة الذهبية في موسم الحصاد", "بريق حجر الجلوستون المشع في الظلام", "لون النحل النشيط في غابة الزهور"
            ),
            "purple", List.of(
                "لون سحر الكتب التي تمنح القوة", "بوابة النهاية التي تناديك للعبور", "لون فاكهة الكورس التي تكسر المكان",
                "وميض حجر الأميثيست في الكهوف", "لون جرعة الشفاء الغامضة", "بريق حراشف التنين الأسطوري"
            )
    );

    public String startBomb(long userId, Difficulty difficulty, Guild guild, Member organizer, net.dv8tion.jda.api.interactions.InteractionHook hook) {
        List<String> wireIds = new ArrayList<>(WIRE_COLORS.keySet());
        Collections.shuffle(wireIds);
        String correct = wireIds.get(0);
        
        userCorrectWire.put(userId, correct);
        userRewards.put(userId, (long) difficulty.reward);
        userGuilds.put(userId, guild.getIdLong());
        userDifficulty.put(userId, difficulty);
        userMentions.put(userId, organizer.getAsMention());

        String hint = HARD_HINTS.get(correct).get(new Random().nextInt(3));
        userHints.put(userId, hint);

        // LOGGING
        String logDetails = String.format("### 💣 فعالية القنبلة: بدء (فردية)\n▫️ **اللاعب:** %s\n▫️ **الصعوبة:** %s\n▫️ **الجائزة:** %d opex\n▫️ **السلك الصحيح:** %s", 
                organizer.getAsMention(), difficulty.displayName, difficulty.reward, WIRE_COLORS.get(correct));
        logManager.logEmbed(guild, LogManager.LOG_GAMES, 
                EmbedUtil.createOldLogEmbed("bomb", logDetails, organizer, null, null, EmbedUtil.INFO));

        return hint;
    }

    public void initTimer(long userId, Difficulty difficulty, net.dv8tion.jda.api.interactions.callbacks.IReplyCallback event) {
        final int[] timeLeft = {difficulty.seconds};
        
        java.util.concurrent.ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(() -> {
            timeLeft[0]--;
            
            if (timeLeft[0] <= 0) {
                explode(userId, event.getHook());
                return;
            }

            String body = getBombBody(userMentions.get(userId), userHints.get(userId), difficulty.reward, timeLeft[0]);
            
            event.getHook().editOriginal(new net.dv8tion.jda.api.utils.messages.MessageEditBuilder()
                    .setComponents(EmbedUtil.containerBranded("DEFUSAL", "⚠️ قنبلة الوقت!", body, EmbedUtil.BANNER_MAIN,
                            ActionRow.of(
                                    Button.danger("wire_red_" + userId, "السلك الأحمر").withEmoji(Emoji.fromUnicode("🔴")),
                                    Button.primary("wire_blue_" + userId, "السلك الأزرق").withEmoji(Emoji.fromUnicode("🔵")),
                                    Button.success("wire_green_" + userId, "السلك الأخضر").withEmoji(Emoji.fromUnicode("🟢")),
                                    Button.secondary("wire_yellow_" + userId, "السلك الأصفر").withEmoji(Emoji.fromUnicode("🟡")),
                                    Button.secondary("wire_purple_" + userId, "السلك البنفسجي").withEmoji(Emoji.fromUnicode("🟣"))
                            )))
                    .useComponentsV2(true)
                    .build()).queue(null, e -> cancelTimer(userId));
            
        }, 1, 1, java.util.concurrent.TimeUnit.SECONDS);
        
        userTimers.put(userId, future);
    }

    private String getBombBody(String mention, String hint, int reward, int seconds) {
        String timerFormat = String.format("`=----------------%02d:%02d----------------=`", 0, seconds);
        return timerFormat + "\n\n" +
               String.format("🆘 **تحذير!** تم زرع قنبلة موقوتة خاصة بك يا %s!\n" +
               "يجب قطع سلك واحد لإبطال مفعولها. هناك سلك واحد صحيح والبقية ستفجر المكان!\n\n" +
               "🔍 **التلميح:** `%s`\n\n" +
               "💰 الجائزة: **%d opex**", mention, hint, reward);
    }

    private void cancelTimer(long userId) {
        if (userTimers.containsKey(userId)) {
            userTimers.get(userId).cancel(true);
            userTimers.remove(userId);
        }
        userDifficulty.remove(userId);
        userHints.remove(userId);
        userMentions.remove(userId);
    }

    private void explode(long userId, net.dv8tion.jda.api.interactions.InteractionHook hook) {
        cancelTimer(userId);
        if (!userCorrectWire.containsKey(userId)) return;

        String correct = userCorrectWire.get(userId);
        userCorrectWire.remove(userId);
        userRewards.remove(userId);

        String failMsg = "💥 **بـوووم!** انتهى الوقت وانفجرت القنبلة!\n❌ حظاً أوفر في المرة القادمة.";
        
        hook.editOriginal(new net.dv8tion.jda.api.utils.messages.MessageEditBuilder()
                .setComponents(EmbedUtil.error("BOMB EXPLODED", "`=---------------- 00:00 ----------------=`\n\n" + failMsg))
                .useComponentsV2(true)
                .build()).queue();

        // LOG FAIL
        String logFail = String.format("### 💥 فعالية القنبلة: انفجار (فردية - انتهاء الوقت)\n▫️ **اللاعب:** <@%d>\n▫️ **السلك الصحيح كان:** %s", 
                userId, WIRE_COLORS.get(correct));
        
        Long guildId = userGuilds.get(userId);
        if (guildId != null) {
            net.dv8tion.jda.api.entities.Guild guild = hook.getJDA().getGuildById(guildId);
            if (guild != null) {
                logManager.logEmbed(guild, LogManager.LOG_GAMES, 
                        EmbedUtil.createOldLogEmbed("bomb_timeout", logFail, null, net.dv8tion.jda.api.entities.UserSnowflake.fromId(userId), null, EmbedUtil.DANGER));
            }
        }
        
        userGuilds.remove(userId);
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String cid = event.getComponentId();
        if (!cid.startsWith("wire_")) return;

        String[] parts = cid.split("_");
        if (parts.length < 3) return;

        String color = parts[1];
        long userId = Long.parseLong(parts[2]);

        if (event.getUser().getIdLong() != userId) {
            event.reply("❌ هذه القنبلة ليست لك!").setEphemeral(true).queue();
            return;
        }

        if (!userCorrectWire.containsKey(userId)) {
            event.editMessage("⚠️ هذه القنبلة انتهت صلاحيتها.").setComponents().queue();
            return;
        }

        cancelTimer(userId);
        String correctColor = userCorrectWire.get(userId);
        long reward = userRewards.get(userId);
        userGuilds.remove(userId);

        if (color.equals(correctColor)) {
            userCorrectWire.remove(userId);
            userRewards.remove(userId);
            
            economyService.addBalance(event.getUser().getId(), event.getGuild().getId(), (int) reward);
            achievementService.updateStats(userId, event.getGuild(), s -> s.setBombWins(s.getBombWins() + 1));

            String successMsg = String.format("✅ تهانينا <@%s>! لقد قمت بقطع السلك الصحيح (%s) وأبطلت مفعول القنبلة بنجاح!\n💰 حصلت على **%d opex**", 
                    event.getUser().getId(), WIRE_COLORS.get(color), reward);
            
            event.editMessage(new net.dv8tion.jda.api.utils.messages.MessageEditBuilder()
                    .setComponents(EmbedUtil.success("BOMB DEFUSED", "`=---------------- SAFE ----------------=`\n\n" + successMsg))
                    .useComponentsV2(true)
                    .build()).queue();

            // LOG WIN
            String logWin = String.format("### 🏆 فعالية القنبلة: فوز (فردية)\n▫️ **الفائز:** <@%s>\n▫️ **الجائزة:** %d opex\n▫️ **السلك:** %s", 
                    event.getUser().getId(), reward, WIRE_COLORS.get(color));
            logManager.logEmbed(event.getGuild(), LogManager.LOG_GAMES, 
                    EmbedUtil.createOldLogEmbed("bomb_win", logWin, event.getMember(), null, null, EmbedUtil.SUCCESS));

        } else {
            userCorrectWire.remove(userId);
            userRewards.remove(userId);
            
            String failMsg = String.format("💥 **بـوووم!** قمت بقطع السلك الخطأ (%s) وانفجرت القنبلة في وجهك!\n❌ حظاً أوفر في المرة القادمة.", 
                    WIRE_COLORS.get(color));
            
            event.editMessage(new net.dv8tion.jda.api.utils.messages.MessageEditBuilder()
                    .setComponents(EmbedUtil.error("BOMB EXPLODED", "`=---------------- BOOM ----------------=`\n\n" + failMsg))
                    .useComponentsV2(true)
                    .build()).queue();

            // LOG FAIL
            String logFail = String.format("### 💥 فعالية القنبلة: انفجار (فردية)\n▫️ **اللاعب:** <@%s>\n▫️ **السلك المقطوع:** %s\n▫️ **السلك الصحيح كان:** %s", 
                    event.getUser().getId(), WIRE_COLORS.get(color), WIRE_COLORS.get(correctColor));
            logManager.logEmbed(event.getGuild(), LogManager.LOG_GAMES, 
                    EmbedUtil.createOldLogEmbed("bomb_fail", logFail, event.getMember(), null, null, EmbedUtil.DANGER));
        }
    }
}
