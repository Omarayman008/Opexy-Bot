package com.integrafty.opexy.command;

import com.integrafty.opexy.command.base.SlashCommand;
import com.integrafty.opexy.service.EconomyService;
import com.integrafty.opexy.service.event.AchievementService;
import com.integrafty.opexy.utils.EmbedUtil;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ShopCommand extends ListenerAdapter implements SlashCommand {

    private final EconomyService economyService;
    private final AchievementService achievementService;

    @Override
    public String getName() {
        return "shop";
    }

    @Override
    public SlashCommandData getCommandData() {
        return Commands.slash("shop", "فتح متجر أوبكس لشراء ميزات الألعاب");
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        String body = """
                مرحباً بك في متجر أوبكس التفاعلي! 🛒
                هنا يمكنك شراء ميزات خاصة تساعدك في الفعاليات والألعاب.
                
                🛡️ **درع المافيا (Mafia Shield)**
                يحميك من هجوم المافيا لمرة واحدة.
                **السعر:** 5,000 opex
                
                💰 **مضاعف الجوائز (2x Reward)**
                يضاعف جائزتك القادمة في أي فعالية تفوز بها.
                **السعر:** 15,000 opex
                """;

        event.reply(new net.dv8tion.jda.api.utils.messages.MessageCreateBuilder()
                .setComponents(EmbedUtil.containerBranded("SHOP", "متجر الألعاب — Opexy Shop", body, EmbedUtil.BANNER_MAIN,
                        ActionRow.of(
                                Button.primary("buy_shield", "شراء درع 🛡️"),
                                Button.success("buy_double", "شراء مضاعف 💰")
                        )))
                .useComponentsV2(true).build())
                .useComponentsV2(true).queue();
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String id = event.getComponentId();
        if (!id.startsWith("buy_")) return;

        long userId = event.getUser().getIdLong();
        String guildId = event.getGuild().getId();
        long balance = economyService.getBalance(String.valueOf(userId), guildId);

        if (id.equals("buy_shield")) {
            if (balance < 5000) {
                event.reply("❌ رصيدك غير كافٍ! تحتاج إلى 5,000 opex.").setEphemeral(true).queue();
                return;
            }
            economyService.subtractBalance(String.valueOf(userId), guildId, 5000);
            achievementService.updateStats(userId, event.getGuild(), stats -> {
                stats.setShieldCount(stats.getShieldCount() + 1);
            });
            event.reply("✅ تم شراء **درع المافيا** بنجاح! رصيدك الحالي: " + (balance - 5000) + " opex").setEphemeral(true).queue();
        } 
        else if (id.equals("buy_double")) {
            if (balance < 15000) {
                event.reply("❌ رصيدك غير كافٍ! تحتاج إلى 15,000 opex.").setEphemeral(true).queue();
                return;
            }
            achievementService.updateStats(userId, event.getGuild(), stats -> {
                if (stats.isDoubleRewardActive()) {
                    event.reply("⚠️ لديك مضاعف نشط بالفعل! استهلكه أولاً.").setEphemeral(true).queue();
                    return;
                }
                economyService.subtractBalance(String.valueOf(userId), guildId, 15000);
                stats.setDoubleRewardActive(true);
                event.reply("✅ تم شراء **مضاعف الجوائز** بنجاح! رصيدك الحالي: " + (balance - 15000) + " opex").setEphemeral(true).queue();
            });
        }
    }
}
