package com.integrafty.opexy.service.event;

import com.integrafty.opexy.service.EconomyService;
import com.integrafty.opexy.service.LogManager;
import com.integrafty.opexy.utils.EmbedUtil;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
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

        final int reward;
        final int seconds;
        final String displayName;
        Difficulty(int r, int s, String d) { this.reward = r; this.seconds = s; this.displayName = d; }
    }

    private final Map<Long, String> userCorrectWire = new HashMap<>();
    private final Map<Long, Long> userRewards = new HashMap<>();
    private final Map<Long, Long> userGuilds = new HashMap<>();
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
            "red", List.of("دماء التنين المسفوكة على الثلج", "نواة النجم الأحمر في عمق الأرض", "لون حجر الردستون المشتعل بالنبض"),
            "blue", List.of("لون سماء النهاية قبل الغروب", "دموع الغاست المتجمدة في الجحيم", "بريق اللازورد في أعماق المحيطات"),
            "green", List.of("عين الأندر مان الغاضبة تحت ضوء القمر", "لون الزمرد الذي يفتح أبواب القرى", "عشب الغابة الذي لم تطأه قدم"),
            "yellow", List.of("بريق الذهب الذي أعمى ملوك الجحيم", "لون مسحوق البليز المتطاير في القلعة", "شروق الشمس فوق رمال الصحراء"),
            "purple", List.of("لون سحر الكتب التي تمنح القوة", "بوابة النهاية التي تناديك للعبور", "لون فاكهة الكورس التي تكسر المكان")
    );

    public String startBomb(long userId, Difficulty difficulty, Guild guild, Member organizer, net.dv8tion.jda.api.interactions.InteractionHook hook) {
        List<String> wireIds = new ArrayList<>(WIRE_COLORS.keySet());
        Collections.shuffle(wireIds);
        String correct = wireIds.get(0);
        
        userCorrectWire.put(userId, correct);
        userRewards.put(userId, (long) difficulty.reward);
        userGuilds.put(userId, guild.getIdLong());

        String hint = HARD_HINTS.get(correct).get(new Random().nextInt(3));

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

            String timerFormat = String.format("`=----------------%02d:%02d----------------=`", 0, timeLeft[0]);
            event.getHook().editOriginal(timerFormat).queue(null, e -> cancelTimer(userId));
            
        }, 1, 1, java.util.concurrent.TimeUnit.SECONDS);
        
        userTimers.put(userId, future);
    }

    private void cancelTimer(long userId) {
        if (userTimers.containsKey(userId)) {
            userTimers.get(userId).cancel(true);
            userTimers.remove(userId);
        }
    }

    private void explode(long userId, net.dv8tion.jda.api.interactions.InteractionHook hook) {
        cancelTimer(userId);
        if (!userCorrectWire.containsKey(userId)) return;

        String correct = userCorrectWire.get(userId);
        userCorrectWire.remove(userId);
        userRewards.remove(userId);

        String failMsg = "💥 **بـوووم!** انتهى الوقت وانفجرت القنبلة!\n❌ حظاً أوفر في المرة القادمة.";
        
        hook.editOriginal(new MessageEditBuilder()
                .setContent("`=---------------- 00:00 ----------------=`")
                .setComponents(EmbedUtil.error("BOMB EXPLODED", failMsg))
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
            
            event.editMessage(new MessageEditBuilder()
                    .setContent("`=---------------- SAFE ----------------=`")
                    .setComponents(EmbedUtil.success("BOMB DEFUSED", successMsg))
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
            
            event.editMessage(new MessageEditBuilder()
                    .setContent("`=---------------- BOOM ----------------=`")
                    .setComponents(EmbedUtil.error("BOMB EXPLODED", failMsg))
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
