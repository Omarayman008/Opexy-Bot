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
                return List.of(Commands.slash("bomb", "بدء لعبة تفكيك القنبلة (فردية)")
                                .addOptions(new OptionData(OptionType.INTEGER, "reward", "قيمة الجائزة بالـ opex",
                                                true)));
        }

        @Override
        public void execute(SlashCommandInteractionEvent event) {
                if (!event.getName().equals("bomb"))
                        return;

                int reward = event.getOption("reward").getAsInt();

                long userId = event.getUser().getIdLong();
                String hint = bombManager.startBomb(userId, reward, event.getGuild(), event.getMember());

                String body = String.format("🆘 **تحذير!** تم زرع قنبلة موقوتة خاصة بك يا %s!\n" +
                                "يجب قطع سلك واحد لإبطال مفعولها. هناك سلك واحد صحيح والبقية ستفجر المكان!\n\n" +
                                "🔍 **التلميح:** `%s`\n\n" +
                                "💰 الجائزة: **%d opex**", event.getUser().getAsMention(), hint, reward);

                event.reply(new MessageCreateBuilder()
                                .setComponents(EmbedUtil.containerBranded("DEFUSAL", "⚠️ قنبلة الوقت!", body,
                                                EmbedUtil.BANNER_MAIN,
                                                ActionRow.of(
                                                                Button.danger("wire_red_" + userId, "السلك الأحمر")
                                                                                .withEmoji(Emoji.fromUnicode("🔴")),
                                                                Button.primary("wire_blue_" + userId, "السلك الأزرق")
                                                                                .withEmoji(Emoji.fromUnicode("🔵")),
                                                                Button.success("wire_green_" + userId, "السلك الأخضر")
                                                                                .withEmoji(Emoji.fromUnicode("🟢")),
                                                                Button.secondary("wire_yellow_" + userId,
                                                                                "السلك الأصفر")
                                                                                .withEmoji(Emoji.fromUnicode("🟡")),
                                                                Button.secondary("wire_purple_" + userId,
                                                                                "السلك البنفسجي")
                                                                                .withEmoji(Emoji.fromUnicode("🟣")))))
                                .useComponentsV2(true)
                                .build()).queue();
        }
}
