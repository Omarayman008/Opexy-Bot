package com.integrafty.opexy.command.event;

import com.integrafty.opexy.command.base.MultiSlashCommand;
import com.integrafty.opexy.service.event.BombManager;
import com.integrafty.opexy.service.event.EventManager;
import com.integrafty.opexy.utils.EmbedUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BombCommand implements MultiSlashCommand {

    private final EventManager eventManager;
    private final BombManager bombManager;

    public BombCommand(EventManager eventManager, BombManager bombManager) {
        this.eventManager = eventManager;
        this.bombManager = bombManager;
    }

    @Override
    public List<SlashCommandData> getCommandDataList() {
        return List.of(Commands.slash("bomb", "بدء فعالية تفكيك القنبلة (Staff Only)")
                .addOptions(new OptionData(OptionType.INTEGER, "reward", "قيمة الجائزة بالـ opex", true))
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR)));
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("bomb")) return;

        if (eventManager.isEventActive()) {
            event.replyEmbeds(EmbedUtil.error("EVENT ACTIVE", "هناك فعالية جارية حالياً، يرجى انتظار انتهائها.").getEmbeds().get(0))
                    .setEphemeral(true).queue();
            return;
        }

        int reward = event.getOption("reward").getAsInt();
        eventManager.startGroupEvent("قنبلة الوقت");
        
        String hint = bombManager.startBomb(reward, event.getGuild(), event.getMember());

        String body = String.format("🆘 **تحذير!** تم زرع قنبلة موقوتة في الشات!\n" +
                "يجب قطع سلك واحد لإبطال مفعولها. هناك سلك واحد صحيح والبقية ستفجر المكان!\n\n" +
                "🔍 **التلميح:** `%s`\n\n" +
                "💰 الجائزة: **%d opex**", hint, reward);

        event.reply(new MessageCreateBuilder()
                .setComponents(EmbedUtil.containerBranded("DEFUSAL", "⚠️ قنبلة الوقت!", body, EmbedUtil.BANNER_MAIN,
                        ActionRow.of(
                                Button.danger("wire_red", "السلك الأحمر").withEmoji(Emoji.fromUnicode("🔴")),
                                Button.primary("wire_blue", "السلك الأزرق").withEmoji(Emoji.fromUnicode("🔵")),
                                Button.success("wire_green", "السلك الأخضر").withEmoji(Emoji.fromUnicode("🟢")),
                                Button.secondary("wire_yellow", "السلك الأصفر").withEmoji(Emoji.fromUnicode("🟡")),
                                Button.secondary("wire_purple", "السلك البنفسجي").withEmoji(Emoji.fromUnicode("🟣"))
                        )))
                .build()).queue();
    }
}
