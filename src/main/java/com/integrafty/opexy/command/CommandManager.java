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

@Component
@RequiredArgsConstructor
@Slf4j
public class CommandManager {

    private final JDA jda;
    private final DiscordEventListener discordEventListener;
    private final List<SlashCommand> commands;

    @PostConstruct
    public void init() {
        jda.addEventListener(discordEventListener);
        registerCommands();
    }

    private void registerCommands() {
        log.info("Registering {} modular slash commands...", commands.size());
        
        var commandDataList = commands.stream()
                .map(SlashCommand::getCommandData)
                .collect(Collectors.toList());

        // Add dummy commands for future features
        commandDataList.add(Commands.slash("profile", "عرض ملف العضو").addOption(OptionType.USER, "user", "العضو", false));
        commandDataList.add(Commands.slash("balance", "عرض رصيد العملات"));
        commandDataList.add(Commands.slash("daily", "مكافأة يومية"));

        jda.updateCommands().addCommands(commandDataList).queue(
            success -> log.info("Successfully registered all modular commands."),
            error -> log.error("Failed to register commands.", error)
        );
    }
}
