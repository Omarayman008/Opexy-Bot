package com.integrafty.opexy.listener;

import com.integrafty.opexy.entity.GuildConfigEntity;
import com.integrafty.opexy.repository.GuildConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import com.integrafty.opexy.utils.EmbedUtil;
import net.dv8tion.jda.api.components.container.Container;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class DiscordEventListener extends ListenerAdapter {

    private final GuildConfigRepository guildConfigRepository;

    @Override
    public void onReady(ReadyEvent event) {
        log.info("Bot is ready! Logged in as {}", event.getJDA().getSelfUser().getAsTag());
    }

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        Guild guild = event.getGuild();
        
        // Auto-role for humans
        if (!event.getUser().isBot()) {
            net.dv8tion.jda.api.entities.Role humanRole = guild.getRoleById("1488278492650143854");
            if (humanRole != null) guild.addRoleToMember(event.getMember(), humanRole).queue();
        } else {
            // Auto-role for bots
            net.dv8tion.jda.api.entities.Role botRole = guild.getRoleById("1487878039177269248");
            if (botRole != null) guild.addRoleToMember(event.getMember(), botRole).queue();
        }
    }

    @Override
    public void onGuildMemberRemove(GuildMemberRemoveEvent event) {
        // Logging is now handled by WelcomeListener using LogManager
    }
}
