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

    private final EventManager eventManager;
    private final AchievementService achievementService;
    private final EconomyService economyService;
    private final LogManager logManager;

    private final Map<Long, String> userCorrectWire = new HashMap<>();
    private final Map<Long, Long> userRewards = new HashMap<>();

    private static final Map<String, String> WIRE_COLORS = Map.of(
            "red", "الأحمر",
            "blue", "الأزرق",
            "green", "الأخضر",
            "yellow", "الأصفر",
            "purple", "البنفسجي"
    );

    private static final Map<String, List<String>> HARD_HINTS = Map.of(
            "red", List.of("لون دماء التنين", "لون حجر الردستون المشتعل", "لون الوردة المحرمة"),
            "blue", List.of("لون سماء الليل الصافية", "لون حجر اللازورد النادر", "لون المحيط العميق"),
            "green", List.of("لون الغابة المطيرة", "لون المروج الخضراء", "لون الزمرد الثمين"),
            "yellow", List.of("لون بريق الذهب الخالص", "لون عيون البليز", "لون الرمال تحت الشمس"),
            "purple", List.of("لون فاكهة الكورس", "لون بوابة النهاية", "لون سحر الكتب القديمة")
    );

    public String startBomb(long userId, long rewardAmount, Guild guild, Member organizer) {
        List<String> wireIds = new ArrayList<>(WIRE_COLORS.keySet());
        Collections.shuffle(wireIds);
        String correct = wireIds.get(0);
        
        userCorrectWire.put(userId, correct);
        userRewards.put(userId, rewardAmount);

        String hint = HARD_HINTS.get(correct).get(new Random().nextInt(3));

        // LOGGING
        String logDetails = String.format("### 💣 فعالية القنبلة: بدء (فردية)\n▫️ **اللاعب:** %s\n▫️ **الجائزة:** %d opex\n▫️ **السلك الصحيح:** %s\n▫️ **التلميح:** %s", 
                organizer.getAsMention(), rewardAmount, WIRE_COLORS.get(correct), hint);
        logManager.logEmbed(guild, LogManager.LOG_GAMES, 
                EmbedUtil.createOldLogEmbed("bomb", logDetails, organizer, null, null, EmbedUtil.INFO));

        return hint;
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String cid = event.getComponentId();
        if (!cid.startsWith("wire_")) return;

        // format: wire_COLOR_USERID
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

        String correctColor = userCorrectWire.get(userId);
        long reward = userRewards.get(userId);

        if (color.equals(correctColor)) {
            userCorrectWire.remove(userId);
            userRewards.remove(userId);
            
            String winnerId = event.getUser().getId();
            economyService.addBalance(winnerId, event.getGuild().getId(), (int) reward);
            achievementService.updateStats(Long.parseLong(winnerId), event.getGuild(), s -> s.setBombWins(s.getBombWins() + 1));

            String successMsg = String.format("✅ تهانينا <@%s>! لقد قمت بقطع السلك الصحيح (%s) وأبطلت مفعول القنبلة بنجاح!\n💰 حصلت على **%d opex**", 
                    winnerId, WIRE_COLORS.get(color), reward);
            
            event.editMessage(new MessageEditBuilder()
                    .setComponents(EmbedUtil.success("BOMB DEFUSED", successMsg))
                    .useComponentsV2(true)
                    .build()).queue();

            // LOG WIN
            String logWin = String.format("### 🏆 فعالية القنبلة: فوز (فردية)\n▫️ **الفائز:** <@%s>\n▫️ **الجائزة:** %d opex\n▫️ **السلك:** %s", 
                    winnerId, reward, WIRE_COLORS.get(color));
            logManager.logEmbed(event.getGuild(), LogManager.LOG_GAMES, 
                    EmbedUtil.createOldLogEmbed("bomb_win", logWin, event.getMember(), null, null, EmbedUtil.SUCCESS));

        } else {
            userCorrectWire.remove(userId);
            userRewards.remove(userId);
            
            String failMsg = String.format("💥 **بـوووم!** قمت بقطع السلك الخطأ (%s) وانفجرت القنبلة في وجهك!\n❌ حظاً أوفر في المرة القادمة.", 
                    WIRE_COLORS.get(color));
            
            event.editMessage(new MessageEditBuilder()
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
