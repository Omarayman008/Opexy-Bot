package com.integrafty.opexy.command;

import com.integrafty.opexy.command.base.SlashCommand;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import com.integrafty.opexy.utils.EmbedUtil;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.Permission;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.time.Instant;

@Component
public class TicketCommand implements SlashCommand {

    @Override
    public String getName() {
        return "tickets";
    }

    @Override
    public SlashCommandData getCommandData() {
        return Commands.slash("tickets", "إرسـال لـوحـة الـتـذاكـر")
                .addOption(OptionType.CHANNEL, "channel", "الـروم الـمـراد إرسـال الـلـوحـة فـيـه (اخـتـيـاري)", false);
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        if (!event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
            event.reply("❌ لا تـمـلـك صـلاحـيـة لاسـتـخـدام هـذا الأمـر.").setEphemeral(true).queue();
            return;
        }

        String rules = "### 📜 قـوانـيـن وشـروط الـدعـم الـفـنـي\n\n" +
                "**الاحـتـرام الـمـتـبـادل** — يـرجـى احـتـرام جـمـيـع أعـضـاء الإدارة. أي إسـاءة قـد تـعـرضـك لـلـحظـر الـنـهـائـي.\n\n" +
                "**تـذكـرة واحـدة** — يـرجـى فـتـح تـذكـرة واحـدة فـقـط لـمـشـكـلـتـك وعـدم الـتـكـرار مـطـلـقـاً.\n\n" +
                "**الـوضـوح** — اشـرح مـشـكـلـتـك بـالـكـامـل فـور فـتـح الـتـذكـرة لـنـسـرع فـي خـدمـتـك الـفـورـيـة.\n\n" +
                "**الـمـنـشـن** — يـمـنـع عـمـل مـنـشـن (Ping) لـلإدارة داخـل الـتـذكـرة، سـنـقـوم بـالـرد بـأقـرب وقـت مـمـكـن.\n\n" +
                "يـرجـى اخـتـيـار الـقـسـم الـمـنـاسـب مـن الأزرار بـالأـسـفـل:";

        ActionRow buttons = ActionRow.of(
            Button.secondary("ticket_support", "الـدعم الـفـنـي"),
            Button.secondary("ticket_complaint", "الـشـكـاوى"),
            Button.secondary("ticket_hire", "الـتـقـديـم عـلـى الإدارة")
        );

        Container container = EmbedUtil.containerBranded(
                "TICKETS", 
                "Support Center", 
                rules, 
                EmbedUtil.BANNER_TICKETS_MENU, 
                buttons
        );

        net.dv8tion.jda.api.entities.channel.middleman.MessageChannel targetChannel = event.getChannel();
        if (event.getOption("channel") != null) {
            targetChannel = event.getOption("channel").getAsChannel().asGuildMessageChannel();
        }

        targetChannel.sendMessageComponents(container).queue();

        Container success = EmbedUtil.success("الإمـدادات", "تـم إرسـال لـوحـة الـتـذاكـر بـنـجـاح فـي " + targetChannel.getAsMention());
        event.replyComponents(success).setEphemeral(true).queue();
    }
}
