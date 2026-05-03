package com.integrafty.opexy.command;

import com.integrafty.opexy.command.base.SlashCommand;
import com.integrafty.opexy.command.base.MultiSlashCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
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
    private Map<String, SlashCommandData> commandDataMap;

    private static final String ALLOWED_CHANNEL_ID = "1487140532965867600";

    @PostConstruct
    public void init() {
        jda.addEventListener(this);
        commandMap = new java.util.HashMap<>();
        commandDataMap = new java.util.HashMap<>();
        
        // Register single commands
        for (SlashCommand cmd : commands) {
            commandMap.put(cmd.getName(), cmd);
            commandDataMap.put(cmd.getName(), cmd.getCommandData());
        }
        
        // Register multi commands
        for (MultiSlashCommand mcmd : multiCommands) {
            for (var data : mcmd.getCommandDataList()) {
                commandMap.put(data.getName(), mcmd);
                commandDataMap.put(data.getName(), data);
            }
        }
        
        log.info("Initialized CommandHandler with {} single and {} multi-command modules.", commands.size(), multiCommands.size());
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        String commandName = event.getName();
        
        Object command = commandMap.get(commandName);
        SlashCommandData data = commandDataMap.get(commandName);

        if (command != null) {
            // Channel Restriction Check for regular members
            if (!isStaff(event.getMember()) && !event.getChannel().getId().equals(ALLOWED_CHANNEL_ID)) {
                // If the command is a public command (no default permissions set or enabled for all)
                if (data != null && (data.getDefaultPermissions() == null || data.getDefaultPermissions().equals(net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions.ENABLED))) {
                    event.reply("❌ عـــذراً، هـــذا الأمـــر مـــتـــاح فـــقـــط فـــي الـــروم الـــمـــخـــصـــص: <#" + ALLOWED_CHANNEL_ID + ">")
                            .setEphemeral(true)
                            .queue();
                    return;
                }
            }

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
    private boolean isStaff(Member member) {
        if (member == null) return false;
        return member.hasPermission(Permission.ADMINISTRATOR) || 
               member.hasPermission(Permission.MANAGE_SERVER) || 
               member.hasPermission(Permission.KICK_MEMBERS);
    }
}
