package com.integrafty.opexy.command;

import com.integrafty.opexy.command.base.SlashCommand;
import com.integrafty.opexy.listener.DiscordEventListener;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.events.session.ReadyEvent;

@Component
@RequiredArgsConstructor
@Slf4j
public class CommandManager extends ListenerAdapter {

    private final JDA jda;
    private final DiscordEventListener discordEventListener;
    private final List<SlashCommand> commands;

    @PostConstruct
    public void init() {
        jda.addEventListener(discordEventListener);
        jda.addEventListener(this); // Register self to listen for ReadyEvent
    }

    @Override
    public void onReady(ReadyEvent event) {
        log.info("JDA is ready, registering commands...");
        registerCommands();
    }

    private void registerCommands() {
        log.info("Registering {} modular slash commands...", commands.size());
        
        var commandDataList = commands.stream()
                .map(SlashCommand::getCommandData)
                .collect(Collectors.toList());

        // Fetch Guild ID from config (or environment)
        String guildId = System.getenv("DISCORD_GUILD_ID");
        if (guildId != null && !guildId.isEmpty()) {
            net.dv8tion.jda.api.entities.Guild guild = jda.getGuildById(guildId);
            if (guild != null) {
                // Clear global commands to avoid duplicates (shown in user screenshot)
                jda.updateCommands().addCommands().queue();
                
                guild.updateCommands().addCommands(commandDataList).queue(
                    success -> log.info("Successfully registered all modular commands to guild: {}", guild.getName()),
                    error -> log.error("Failed to register commands to guild.", error)
                );
            } else {
                log.warn("Guild with ID {} not found. Falling back to global commands.", guildId);
                jda.updateCommands().addCommands(commandDataList).queue();
            }
        } else {
            jda.updateCommands().addCommands(commandDataList).queue();
        }
    }
}
