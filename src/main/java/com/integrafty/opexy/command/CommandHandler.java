package com.integrafty.opexy.command;

import com.integrafty.opexy.command.base.SlashCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.JDA;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class CommandHandler extends ListenerAdapter {

    private final JDA jda;
    private final List<SlashCommand> commands;
    private Map<String, SlashCommand> commandMap;

    @PostConstruct
    public void init() {
        jda.addEventListener(this);
        // Map commands by name for fast lookup
        commandMap = commands.stream().collect(Collectors.toMap(SlashCommand::getName, c -> c));
        log.info("Initialized CommandHandler with {} modular commands.", commands.size());
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        String commandName = event.getName();
        
        SlashCommand command = commandMap.get(commandName);
        if (command != null) {
            log.info("Executing modular command: {} for user {}", commandName, event.getUser().getAsTag());
            command.execute(event);
        } else {
            // Default response for unimplemented commands
            event.reply("❌ عذراً، هذا الأمر (" + commandName + ") غير مدعوم حالياً.").setEphemeral(true).queue();
        }
    }
}
