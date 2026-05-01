package com.integrafty.opexy.listener;

import com.integrafty.opexy.entity.GuildConfigEntity;
import com.integrafty.opexy.repository.GuildConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
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

import java.awt.Color;
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
        Optional<GuildConfigEntity> configOpt = guildConfigRepository.findById(guild.getId());

        if (configOpt.isPresent() && configOpt.get().getWelcomeChannelId() != null) {
            TextChannel welcomeChannel = guild.getTextChannelById(configOpt.get().getWelcomeChannelId());
            if (welcomeChannel != null) {
                String body = "أهلاً بك " + event.getMember().getAsMention() + "!\nأنت العضو رقم **" + guild.getMemberCount() + "**";
                Container welcome = EmbedUtil.containerBranded("WELCOME", "مرحباً بك في " + guild.getName(), body, EmbedUtil.BANNER_WELCOME);
                
                MessageCreateBuilder builder = new MessageCreateBuilder();
                builder.setComponents(welcome);
                builder.useComponentsV2(true);

                welcomeChannel.sendMessage(builder.build()).useComponentsV2(true).queue();
            }
        }
        
        // Auto-role
        guild.addRoleToMember(event.getMember(), guild.getRoleById("1488278492650143854")).queue();
        
        // TODO: Send DM with verify link and server rules
    }

    @Override
    public void onGuildMemberRemove(GuildMemberRemoveEvent event) {
        Guild guild = event.getGuild();
        Optional<GuildConfigEntity> configOpt = guildConfigRepository.findById(guild.getId());

        if (configOpt.isPresent() && configOpt.get().getLogChannelId() != null) {
            TextChannel logChannel = guild.getTextChannelById(configOpt.get().getLogChannelId());
            if (logChannel != null) {
                String body = "العضو: " + event.getUser().getAsMention() + " (" + event.getUser().getName() + ")";
                Container leave = EmbedUtil.containerBranded("LOGS", "عضو غادر السيرفر", body, EmbedUtil.BANNER_MAIN);
                
                MessageCreateBuilder builder = new MessageCreateBuilder();
                builder.setComponents(leave);
                builder.useComponentsV2(true);

                logChannel.sendMessage(builder.build()).useComponentsV2(true).queue();
            }
        }
    }
}
