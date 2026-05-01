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
        return Commands.slash("tickets", "إدارة نظام التذاكر")
                .addSubcommands(
                    new net.dv8tion.jda.api.interactions.commands.build.SubcommandData("setup", "إرسال لوحة التذاكر")
                        .addOption(OptionType.CHANNEL, "channel", "الروم المراد إرسال اللوحة فيه (اختياري)", false),
                    new net.dv8tion.jda.api.interactions.commands.build.SubcommandData("add", "إضافة عضو للتذكرة")
                        .addOption(OptionType.USER, "user", "العضو المراد إضافته", true),
                    new net.dv8tion.jda.api.interactions.commands.build.SubcommandData("remove", "إزالة عضو من التذكرة")
                        .addOption(OptionType.USER, "user", "العضو المراد إزالته", true),
                    new net.dv8tion.jda.api.interactions.commands.build.SubcommandData("rename", "تغيير اسم التذكرة")
                        .addOption(OptionType.STRING, "name", "الاسم الجديد", true),
                    new net.dv8tion.jda.api.interactions.commands.build.SubcommandData("close", "إغلاق التذكرة الحالية"),
                    new net.dv8tion.jda.api.interactions.commands.build.SubcommandData("claim", "استلام التذكرة"),
                    new net.dv8tion.jda.api.interactions.commands.build.SubcommandData("unclaim", "إلغاء استلام التذكرة")
                );
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        String subcommand = event.getSubcommandName();
        if (subcommand == null) return;

        // Everyone can maybe use some commands, but setup is admin only
        if (subcommand.equals("setup") && !event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
            event.reply("❌ لا تملك صلاحية لاستخدام هذا الأمر.").setEphemeral(true).queue();
            return;
        }

        switch (subcommand) {
            case "setup" -> handleSetup(event);
            case "add" -> handleAdd(event);
            case "remove" -> handleRemove(event);
            case "rename" -> handleRename(event);
            case "close" -> handleClose(event);
            case "claim" -> handleClaim(event);
            case "unclaim" -> handleUnclaim(event);
        }
    }
    
    public void handleSetup(SlashCommandInteractionEvent event) {
        String rules = "### 📜 قـوانـيـن وشـروط الـدعـم الـفـنـي\n\n" +
                "**الاحـتـرام الـمـتـبـادل** — يـرجـى احـتـرام جـمـيـع أعـضـاء الإدارة. أي إسـاءة قـد تـعـرضـك لـلـحـظـر الـنـهـائـي.\n\n" +
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

        MessageCreateBuilder builder = new MessageCreateBuilder();
        builder.setComponents(container);
        builder.useComponentsV2(true);

        net.dv8tion.jda.api.entities.channel.middleman.MessageChannel targetChannel = event.getChannel();
        if (event.getOption("channel") != null) {
            targetChannel = event.getOption("channel").getAsChannel().asGuildMessageChannel();
        }

        targetChannel.sendMessage(builder.build()).useComponentsV2(true).queue();

        Container success = EmbedUtil.success("الإمـدادات", "تـم إرسـال لـوحـة الـتـذاكـر بـنـجـاح فـي " + targetChannel.getAsMention());
        MessageCreateBuilder successBuilder = new MessageCreateBuilder();
        successBuilder.setComponents(success);
        successBuilder.useComponentsV2(true);
        
        event.reply(successBuilder.build()).setEphemeral(true).useComponentsV2(true).queue();
    }

    private void handleAdd(SlashCommandInteractionEvent event) {
        net.dv8tion.jda.api.entities.Member target = event.getOption("user").getAsMember();
        net.dv8tion.jda.api.entities.channel.concrete.TextChannel channel = event.getChannel().asTextChannel();
        
        channel.getManager().putPermissionOverride(target, java.util.EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND), null).queue();
        event.reply("✅ تم إضافة " + target.getAsMention() + " إلى التذكرة.").queue();
    }

    private void handleRemove(SlashCommandInteractionEvent event) {
        net.dv8tion.jda.api.entities.Member target = event.getOption("user").getAsMember();
        net.dv8tion.jda.api.entities.channel.concrete.TextChannel channel = event.getChannel().asTextChannel();
        
        channel.getManager().putPermissionOverride(target, null, java.util.EnumSet.of(Permission.VIEW_CHANNEL)).queue();
        event.reply("❌ تم إزالة " + target.getAsMention() + " من التذكرة.").queue();
    }

    private void handleRename(SlashCommandInteractionEvent event) {
        String newName = event.getOption("name").getAsString();
        event.getChannel().asTextChannel().getManager().setName(newName).queue();
        event.reply("✅ تم تغيير اسم التذكرة إلى: " + newName).queue();
    }

    private void handleClose(SlashCommandInteractionEvent event) {
        // This will be handled by the listener if we want to reuse logic, 
        // but for now let's just trigger a delete or rename
        event.reply("🔒 سيتم إغلاق التذكرة...").queue();
        event.getChannel().asTextChannel().getManager().setName(event.getChannel().getName() + "-c").queue();
    }

    private void handleClaim(SlashCommandInteractionEvent event) {
        event.reply("✅ تم استلام التذكرة بواسطة: " + event.getMember().getAsMention()).queue();
    }

    private void handleUnclaim(SlashCommandInteractionEvent event) {
        event.reply("🔓 تم إلغاء استلام التذكرة.").queue();
    }
}
