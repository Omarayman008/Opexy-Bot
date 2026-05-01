package com.integrafty.opexy.command;

import com.integrafty.opexy.listener.DiscordEventListener;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CommandManager {

    private final JDA jda;
    private final DiscordEventListener discordEventListener;

    @PostConstruct
    public void init() {
        jda.addEventListener(discordEventListener);
        registerCommands();
    }

    private void registerCommands() {
        log.info("Registering slash commands...");
        jda.updateCommands().addCommands(
            // Admin Commands
            Commands.slash("warn", "تحذير عضو")
                .addOption(OptionType.USER, "user", "العضو المراد تحذيره", true)
                .addOption(OptionType.STRING, "reason", "سبب التحذير", false),
            
            Commands.slash("mute", "كتم عضو")
                .addOption(OptionType.USER, "user", "العضو المراد كتمه", true)
                .addOption(OptionType.STRING, "time", "مدة الكتم (مثال: 10m, 1h)", true)
                .addOption(OptionType.STRING, "reason", "السبب", false),
            
            Commands.slash("ban", "حظر عضو")
                .addOption(OptionType.USER, "user", "العضو المراد حظره", true)
                .addOption(OptionType.STRING, "reason", "السبب", false)
                .addOption(OptionType.INTEGER, "days", "أيام حذف الرسائل", false),
                
            Commands.slash("kick", "طرد عضو")
                .addOption(OptionType.USER, "user", "العضو المراد طرده", true)
                .addOption(OptionType.STRING, "reason", "السبب", false),

            Commands.slash("purge", "مسح رسائل")
                .addOption(OptionType.INTEGER, "amount", "عدد الرسائل (1-100)", true),

            // Economy Commands
            Commands.slash("profile", "عرض ملف العضو")
                .addOption(OptionType.USER, "user", "العضو", false),
                
            Commands.slash("balance", "عرض رصيد العملات"),
            
            Commands.slash("daily", "مكافأة يومية"),
            
            // Setup Command
            Commands.slash("setup", "إعداد البوت")
                .addOption(OptionType.STRING, "category", "الفئة (channels, roles, etc)", true)
        ).queue(
            success -> log.info("Successfully registered commands."),
            error -> log.error("Failed to register commands.", error)
        );
    }
}
