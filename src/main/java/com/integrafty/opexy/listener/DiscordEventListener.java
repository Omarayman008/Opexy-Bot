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
                EmbedBuilder embed = new EmbedBuilder()
                        .setColor(Color.decode("#5865F2"))
                        .setTitle("👋 مرحباً بك في " + guild.getName())
                        .setDescription("أهلاً بك " + event.getMember().getAsMention() + "!\nأنت العضو رقم **" + guild.getMemberCount() + "**")
                        .setThumbnail(event.getUser().getEffectiveAvatarUrl())
                        .setImage("https://i.imgur.com/placeholder_welcome.png") // Placeholder image
                        .setFooter("HighCore System");
                
                welcomeChannel.sendMessageEmbeds(embed.build()).queue();
            }
        }
        
        // TODO: Send DM with verify link and server rules
    }

    @Override
    public void onGuildMemberRemove(GuildMemberRemoveEvent event) {
        Guild guild = event.getGuild();
        Optional<GuildConfigEntity> configOpt = guildConfigRepository.findById(guild.getId());

        if (configOpt.isPresent() && configOpt.get().getLogChannelId() != null) {
            TextChannel logChannel = guild.getTextChannelById(configOpt.get().getLogChannelId());
            if (logChannel != null) {
                EmbedBuilder embed = new EmbedBuilder()
                        .setColor(Color.RED)
                        .setTitle("عضو غادر السيرفر")
                        .setDescription("العضو: " + event.getUser().getAsMention() + " (" + event.getUser().getName() + ")")
                        .setThumbnail(event.getUser().getEffectiveAvatarUrl())
                        .setFooter("HighCore Logs");
                
                logChannel.sendMessageEmbeds(embed.build()).queue();
            }
        }
    }
}
