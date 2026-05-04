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
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class BombManager extends ListenerAdapter {

    private final EventManager eventManager;
    private final AchievementService achievementService;
    private final EconomyService economyService;
    private final LogManager logManager;

    private String correctWireId = null;
    private long reward = 5000;
    private boolean active = false;

    private static final Map<String, String> WIRE_COLORS = Map.of(
            "wire_red", "الأحمر",
            "wire_blue", "الأزرق",
            "wire_green", "الأخضر",
            "wire_yellow", "الأصفر",
            "wire_purple", "البنفسجي"
    );

    private static final Map<String, List<String>> HARD_HINTS = Map.of(
            "wire_red", List.of("لون دماء التنين", "لون حجر الردستون المشتعل", "لون الوردة المحرمة"),
            "wire_blue", List.of("لون سماء الليل الصافية", "لون حجر اللازورد النادر", "لون المحيط العميق"),
            "wire_green", List.of("لون الغابة المطيرة", "لون عين الأندر مان الغاضبة (تقريباً)", "لون الزمرد الثمين"),
            "wire_yellow", List.of("لون بريق الذهب الخالص", "لون عيون البليز", "لون الرمال تحت الشمس"),
            "wire_purple", List.of("لون فاكهة الكورس", "لون بوابة النهاية", "لون سحر الكتب القديمة")
    );

    public String startBomb(long rewardAmount, Guild guild, Member organizer) {
        this.reward = rewardAmount;
        this.active = true;
        
        List<String> wireIds = new ArrayList<>(WIRE_COLORS.keySet());
        Collections.shuffle(wireIds);
        this.correctWireId = wireIds.get(0);

        String hint = HARD_HINTS.get(correctWireId).get(new Random().nextInt(3));

        // LOGGING
        String logDetails = String.format("### 💣 فعالية القنبلة: بدء الفعالية\n▫️ **المنظم:** %s\n▫️ **الجائزة:** %d opex\n▫️ **السلك الصحيح:** %s\n▫️ **التلميح:** %s", 
                organizer.getAsMention(), rewardAmount, WIRE_COLORS.get(correctWireId), hint);
        logManager.logEmbed(guild, LogManager.LOG_GAMES, 
                EmbedUtil.createOldLogEmbed("bomb", logDetails, organizer, null, null, EmbedUtil.INFO));

        return hint;
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        if (!active || !event.getComponentId().startsWith("wire_")) return;

        if (event.getComponentId().equals(correctWireId)) {
            active = false;
            String winnerId = event.getUser().getId();
            economyService.addBalance(winnerId, event.getGuild().getId(), (int) reward);
            achievementService.incrementGameWin(winnerId);
            eventManager.endGroupEvent();

            String successMsg = String.format("✅ تهانينا <@%s>! لقد قمت بقطع السلك الصحيح (%s) وأبطلت مفعول القنبلة بنجاح!\n💰 حصلت على **%d opex**", 
                    winnerId, WIRE_COLORS.get(correctWireId), reward);
            
            event.editMessageEmbeds(EmbedUtil.success("BOMB DEFUSED", successMsg).getEmbeds().get(0))
                    .setComponents().queue();

            // LOG WIN
            String logWin = String.format("### 🏆 فعالية القنبلة: فوز\n▫️ **الفائز:** <@%s>\n▫️ **الجائزة:** %d opex\n▫️ **السلك:** %s", 
                    winnerId, reward, WIRE_COLORS.get(correctWireId));
            logManager.logEmbed(event.getGuild(), LogManager.LOG_GAMES, 
                    EmbedUtil.createOldLogEmbed("bomb_win", logWin, event.getMember(), null, null, EmbedUtil.SUCCESS));

        } else {
            // Wrong wire - BOOM (for that person or ends the game?)
            // Let's make it end the game for everyone - BOOM!
            active = false;
            eventManager.endGroupEvent();
            
            String failMsg = String.format("💥 **بـوووم!** قام <@%s> بقطع السلك الخطأ (%s) وانفجرت القنبلة!\n❌ خسر الجميع في هذه الجولة.", 
                    event.getUser().getId(), WIRE_COLORS.get(event.getComponentId()));
            
            event.editMessageEmbeds(EmbedUtil.error("BOMB EXPLODED", failMsg).getEmbeds().get(0))
                    .setComponents().queue();

            // LOG FAIL
            String logFail = String.format("### 💥 فعالية القنبلة: انفجار\n▫️ **المتسبب:** <@%s>\n▫️ **السلك المقطوع:** %s\n▫️ **السلك الصحيح كان:** %s", 
                    event.getUser().getId(), WIRE_COLORS.get(event.getComponentId()), WIRE_COLORS.get(correctWireId));
            logManager.logEmbed(event.getGuild(), LogManager.LOG_GAMES, 
                    EmbedUtil.createOldLogEmbed("bomb_fail", logFail, event.getMember(), null, null, EmbedUtil.ERROR));
        }
    }
}
