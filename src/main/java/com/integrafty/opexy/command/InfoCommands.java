package com.integrafty.opexy.command;

import com.integrafty.opexy.command.base.MultiSlashCommand;
import com.integrafty.opexy.utils.EmbedUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.components.container.Container;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class InfoCommands implements MultiSlashCommand {

    @Override
    public List<SlashCommandData> getCommandDataList() {
        List<SlashCommandData> list = new ArrayList<>();

        list.add(Commands.slash("profile", "View customizable personal global profile card")
                .addOption(OptionType.USER, "user", "User to view", false));
        
        list.add(Commands.slash("user", "Shows detailed information about a user")
                .addOption(OptionType.USER, "user", "User to view", false));
        
        list.add(Commands.slash("avatar", "Get a user's avatar")
                .addOption(OptionType.USER, "user", "User to view", false));
        
        list.add(Commands.slash("banner", "Get a user's banner")
                .addOption(OptionType.USER, "user", "User to view", false));

        list.add(Commands.slash("server", "Shows information about the server"));
        
        list.add(Commands.slash("roles", "Get a list of server roles and counts"));
        
        list.add(Commands.slash("server-avatar", "Display the server's icon"));
        
        list.add(Commands.slash("server-banner", "Display the server's banner"));

        return list;
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        String name = event.getName().toLowerCase();

        switch (name) {
            case "profile", "user" -> handleUser(event);
            case "avatar" -> handleAvatar(event);
            case "banner" -> handleBanner(event);
            case "server" -> handleServer(event);
            case "roles" -> handleRoles(event);
            case "server-avatar" -> handleServerAvatar(event);
            case "server-banner" -> handleServerBanner(event);
        }
    }

    private void handleUser(SlashCommandInteractionEvent event) {
        Member m = event.getOption("user") != null ? event.getOption("user").getAsMember() : event.getMember();
        if (m == null) return;

        long created = m.getUser().getTimeCreated().toEpochSecond();
        long joined = m.getTimeJoined().toEpochSecond();

        String roles = m.getRoles().stream()
                .map(Role::getAsMention)
                .limit(10)
                .collect(Collectors.joining(" "));
        if (roles.isEmpty()) roles = "None";

        String desc = String.format("""
                ### 👤 Operative Identity
                ▫️ **Profile:** %s
                ▫️ **Identifier:** `%s`
                ▫️ **Status:** `%s`
                
                ### 📅 History
                ▫️ **Registered:** <t:%d:R>
                ▫️ **Joined Station:** <t:%d:R>
                
                ### 🛡️ Authority
                %s
                """, m.getUser().getAsMention(), m.getUser().getId(), m.getOnlineStatus().name(), created, joined, roles);

        reply(event, EmbedUtil.containerBranded("IDENTITY", "User Metrics", desc, m.getUser().getEffectiveAvatarUrl()));
    }

    private void handleAvatar(SlashCommandInteractionEvent event) {
        User u = event.getOption("user") != null ? event.getOption("user").getAsUser() : event.getUser();
        String url = u.getEffectiveAvatarUrl() + "?size=1024";
        reply(event, EmbedUtil.containerBranded("VISUAL", "Avatar Data", "### 🖼️ Profile Avatar\nUser: " + u.getAsMention(), url));
    }

    private void handleBanner(SlashCommandInteractionEvent event) {
        User u = event.getOption("user") != null ? event.getOption("user").getAsUser() : event.getUser();
        u.retrieveProfile().queue(profile -> {
            String url = profile.getBannerUrl();
            if (url == null) {
                replyEphemeral(event, EmbedUtil.error("DATA ERROR", "This user has no banner deployed."));
                return;
            }
            reply(event, EmbedUtil.containerBranded("VISUAL", "Banner Data", "### 🎨 Profile Banner\nUser: " + u.getAsMention(), url + "?size=1024"));
        });
    }

    private void handleServer(SlashCommandInteractionEvent event) {
        Guild g = event.getGuild();
        if (g == null) return;
        String desc = String.format("""
                ### 🏛️ Station Registry
                ▫️ **Name:** `%s`
                ▫️ **Identifier:** `%s`
                ▫️ **Owner:** <@%s>
                
                ### 📊 Statistics
                ▫️ **Personnel:** `%d`
                ▫️ **Established:** <t:%d:D>
                ▫️ **Boost Level:** `%s` (%d boosts)
                """, g.getName(), g.getId(), g.getOwnerId(), g.getMemberCount(), g.getTimeCreated().toEpochSecond(), g.getBoostTier().name(), g.getBoostCount());
        reply(event, EmbedUtil.containerBranded("REGISTRY", "Server Metrics", desc, g.getIconUrl()));
    }

    private void handleRoles(SlashCommandInteractionEvent event) {
        Guild g = event.getGuild();
        String roles = g.getRoles().stream()
                .limit(15)
                .map(r -> r.getAsMention() + " (`" + g.getMembersWithRoles(r).size() + "`)")
                .collect(Collectors.joining("\n"));
        
        reply(event, EmbedUtil.containerBranded("SECURITY", "Clearance Registry", "### 📑 Authority Levels\n" + roles + "\n\n*Showing top 15 roles*", null));
    }

    private void handleServerAvatar(SlashCommandInteractionEvent event) {
        Guild g = event.getGuild();
        if (g.getIconUrl() == null) {
            replyEphemeral(event, EmbedUtil.error("DATA ERROR", "Server has no icon."));
            return;
        }
        reply(event, EmbedUtil.containerBranded("VISUAL", "Station Icon", "### 🏛️ Server Visual Asset", g.getIconUrl() + "?size=1024"));
    }

    private void handleServerBanner(SlashCommandInteractionEvent event) {
        Guild g = event.getGuild();
        if (g.getBannerUrl() == null) {
            replyEphemeral(event, EmbedUtil.error("DATA ERROR", "Server has no banner."));
            return;
        }
        reply(event, EmbedUtil.containerBranded("VISUAL", "Station Banner", "### 🏛️ Server Banner Asset", g.getBannerUrl() + "?size=1024"));
    }

    private void reply(SlashCommandInteractionEvent e, Container c) {
        var msg = new MessageCreateBuilder().setComponents(c).useComponentsV2(true).build();
        e.reply(msg).useComponentsV2(true).queue();
    }

    private void replyEphemeral(SlashCommandInteractionEvent e, Container c) {
        var msg = new MessageCreateBuilder().setComponents(c).useComponentsV2(true).build();
        e.reply(msg).setEphemeral(true).useComponentsV2(true).queue();
    }
}
