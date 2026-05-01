package com.integrafty.opexy.command;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.JDA;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.awt.Color;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class CommandHandler extends ListenerAdapter {

    private final JDA jda;

    @Autowired
    private TicketCommand ticketCommand;

    @PostConstruct
    public void init() {
        jda.addEventListener(this);
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        String commandName = event.getName();
        log.info("User {} executed command: {}", event.getUser().getAsTag(), commandName);

        // All moderation commands require guild context
        if (event.getGuild() == null) {
            event.reply("❌ هذه الأوامر تعمل داخل السيرفر فقط.").setEphemeral(true).queue();
            return;
        }

        switch (commandName) {
            case "warn":
                handleWarn(event);
                break;
            case "mute":
                handleMute(event);
                break;
            case "ban":
                handleBan(event);
                break;
            case "kick":
                handleKick(event);
                break;
            case "purge":
                handlePurge(event);
                break;
            case "profile":
            case "balance":
            case "daily":
                event.reply("⏳ سيتم تفعيل هذا الأمر في المراحل القادمة.").setEphemeral(true).queue();
                break;
            case "setup":
                if (!event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
                    event.reply("❌ لا تملك صلاحية لاستخدام هذا الأمر.").setEphemeral(true).queue();
                    return;
                }
                ticketCommand.handleSetup(event);
                break;
            default:
                event.reply("❌ عذراً، هذا الأمر غير مدعوم حالياً.").setEphemeral(true).queue();
        }
    }

    private void handleWarn(SlashCommandInteractionEvent event) {
        if (!event.getMember().hasPermission(Permission.KICK_MEMBERS)) {
            event.reply("❌ لا تملك صلاحية لاستخدام هذا الأمر.").setEphemeral(true).queue();
            return;
        }

        Member target = event.getOption("user").getAsMember();
        String reason = event.getOption("reason") != null ? event.getOption("reason").getAsString() : "بدون سبب";

        if (target == null) {
            event.reply("❌ لم يتم العثور على العضو.").setEphemeral(true).queue();
            return;
        }

        // TODO: Save warning to database

        EmbedBuilder embed = new EmbedBuilder()
                .setColor(Color.ORANGE)
                .setTitle("⚠️ تم تحذير عضو")
                .addField("العضو", target.getAsMention(), true)
                .addField("المراقب", event.getMember().getAsMention(), true)
                .addField("السبب", reason, false);

        event.replyEmbeds(embed.build()).queue();
        
        target.getUser().openPrivateChannel().queue(pc -> {
            pc.sendMessage("تم تحذيرك في سيرفر " + event.getGuild().getName() + " بسبب: " + reason).queue(null, e -> {});
        });
    }

    private void handleMute(SlashCommandInteractionEvent event) {
        if (!event.getMember().hasPermission(Permission.MODERATE_MEMBERS)) {
            event.reply("❌ لا تملك صلاحية (Timeout) لاستخدام هذا الأمر.").setEphemeral(true).queue();
            return;
        }

        Member target = event.getOption("user").getAsMember();
        String reason = event.getOption("reason") != null ? event.getOption("reason").getAsString() : "بدون سبب";
        
        if (target == null) {
            event.reply("❌ لم يتم العثور على العضو.").setEphemeral(true).queue();
            return;
        }

        // Defaulting to 1 hour for now, parser logic will be added later
        event.getGuild().timeoutFor(target, 1, TimeUnit.HOURS).reason(reason).queue(
            success -> {
                EmbedBuilder embed = new EmbedBuilder()
                        .setColor(Color.RED)
                        .setTitle("🔇 تم كتم (Timeout) عضو")
                        .addField("العضو", target.getAsMention(), true)
                        .addField("المراقب", event.getMember().getAsMention(), true)
                        .addField("المدة", "1 ساعة (مبدئياً)", true)
                        .addField("السبب", reason, false);
                event.replyEmbeds(embed.build()).queue();
            },
            error -> event.reply("❌ حدث خطأ أثناء تطبيق الكتم. تأكد من صلاحيات البوت.").setEphemeral(true).queue()
        );
    }

    private void handleBan(SlashCommandInteractionEvent event) {
        if (!event.getMember().hasPermission(Permission.BAN_MEMBERS)) {
            event.reply("❌ لا تملك صلاحية لاستخدام هذا الأمر.").setEphemeral(true).queue();
            return;
        }

        Member target = event.getOption("user").getAsMember();
        String reason = event.getOption("reason") != null ? event.getOption("reason").getAsString() : "بدون سبب";
        int delDays = event.getOption("days") != null ? event.getOption("days").getAsInt() : 0;

        if (target == null) {
            event.reply("❌ لم يتم العثور على العضو.").setEphemeral(true).queue();
            return;
        }

        event.getGuild().ban(target, delDays, TimeUnit.DAYS).reason(reason).queue(
            success -> {
                EmbedBuilder embed = new EmbedBuilder()
                        .setColor(Color.decode("#8B0000"))
                        .setTitle("🔨 تم حظر عضو")
                        .addField("العضو", target.getUser().getAsTag(), true)
                        .addField("المراقب", event.getMember().getAsMention(), true)
                        .addField("السبب", reason, false);
                event.replyEmbeds(embed.build()).queue();
            },
            error -> event.reply("❌ لم أتمكن من حظر العضو. قد تكون رتبته أعلى مني.").setEphemeral(true).queue()
        );
    }

    private void handleKick(SlashCommandInteractionEvent event) {
        if (!event.getMember().hasPermission(Permission.KICK_MEMBERS)) {
            event.reply("❌ لا تملك صلاحية لاستخدام هذا الأمر.").setEphemeral(true).queue();
            return;
        }

        Member target = event.getOption("user").getAsMember();
        String reason = event.getOption("reason") != null ? event.getOption("reason").getAsString() : "بدون سبب";

        if (target == null) {
            event.reply("❌ لم يتم العثور على العضو.").setEphemeral(true).queue();
            return;
        }

        event.getGuild().kick(target).reason(reason).queue(
            success -> event.reply("✅ تم طرد العضو **" + target.getUser().getAsTag() + "** بنجاح.").queue(),
            error -> event.reply("❌ حدث خطأ. تأكد من أن رتبتي أعلى من رتبة العضو.").setEphemeral(true).queue()
        );
    }

    private void handlePurge(SlashCommandInteractionEvent event) {
        if (!event.getMember().hasPermission(Permission.MESSAGE_MANAGE)) {
            event.reply("❌ لا تملك صلاحية لحذف الرسائل.").setEphemeral(true).queue();
            return;
        }

        int amount = event.getOption("amount").getAsInt();
        if (amount < 1 || amount > 100) {
            event.reply("❌ يرجى إدخال رقم بين 1 و 100.").setEphemeral(true).queue();
            return;
        }

        event.getChannel().getIterableHistory().takeAsync(amount).thenAccept(messages -> {
            event.getChannel().purgeMessages(messages);
            event.reply("✅ تم حذف " + messages.size() + " رسائل.").setEphemeral(true).queue(m -> {
                // Delete the reply after 3 seconds
                m.deleteOriginal().queueAfter(3, TimeUnit.SECONDS);
            });
        });
    }
}
