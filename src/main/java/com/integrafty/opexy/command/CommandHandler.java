package com.integrafty.opexy.command;

import com.integrafty.opexy.command.base.SlashCommand;
import com.integrafty.opexy.command.base.MultiSlashCommand;
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
    private final List<MultiSlashCommand> multiCommands;
    private Map<String, Object> commandMap;

    @PostConstruct
    public void init() {
        jda.addEventListener(this);
        commandMap = new java.util.HashMap<>();
        
        // Register single commands
        for (SlashCommand cmd : commands) {
            commandMap.put(cmd.getName(), cmd);
        }
        
        // Register multi commands
        for (MultiSlashCommand mcmd : multiCommands) {
            for (var data : mcmd.getCommandDataList()) {
                commandMap.put(data.getName(), mcmd);
            }
        }
        
        log.info("Initialized CommandHandler with {} single and {} multi-command modules.", commands.size(), multiCommands.size());
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        String commandName = event.getName();
        
        Object command = commandMap.get(commandName);
        if (command != null) {
            log.info("Executing command: {} for user {}", commandName, event.getUser().getAsTag());
            if (command instanceof SlashCommand sc) sc.execute(event);
            else if (command instanceof MultiSlashCommand msc) msc.execute(event);
        } else {
            // Default response for unimplemented commands
            event.reply("❌ عذراً، هذا الأمر (" + commandName + ") غير مدعوم حالياً.").setEphemeral(true).queue();
        }
    }

    @Override
    public void onCommandAutoCompleteInteraction(net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent event) {
        String commandName = event.getName();
        Object command = commandMap.get(commandName);
        
        if (command instanceof MultiSlashCommand msc) {
            msc.onAutoComplete(event);
        }
    }
}
