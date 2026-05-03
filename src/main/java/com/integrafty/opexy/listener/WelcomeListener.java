package com.integrafty.opexy.listener;

import com.integrafty.opexy.service.LogManager;
import com.integrafty.opexy.service.WelcomeCardService;
import com.integrafty.opexy.utils.EmbedUtil;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.utils.FileUpload;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class WelcomeListener extends ListenerAdapter {

    private final JDA jda;
    private final LogManager logManager;

    @Value("${welcome.channel.id:1487138386258165820}")
    private String welcomeChannelId;

    @PostConstruct
    public void init() {
        jda.addEventListener(this);
    }

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        log.info("Member joined: {} in {}", event.getMember().getUser().getName(), event.getGuild().getName());

        try {
            byte[] welcomeImage = WelcomeCardService.generateWelcomeCard(event.getMember());
            sendWelcomeMessage(event.getMember(), event.getGuild(), welcomeImage);
            sendStartupDM(event.getMember(), welcomeImage);
        } catch (Exception e) {
            log.error("Failed to execute welcome protocol", e);
            sendWelcomeMessage(event.getMember(), event.getGuild(), null);
        }

        logManager.logEmbed(event.getGuild(), LogManager.LOG_JOIN_LEFT, 
            EmbedUtil.brandedEmbed("Member Joined", "A new member has joined: **"
                + event.getMember().getUser().getName() + "** (" + event.getMember().getId() + ")"));
    }

    @Override
    public void onGuildMemberRemove(GuildMemberRemoveEvent event) {
        logManager.logEmbed(event.getGuild(), LogManager.LOG_JOIN_LEFT,
            EmbedUtil.brandedEmbed("Member Left", "A member has left the server: **" 
                + event.getUser().getName() + "** (" + event.getUser().getId() + ")"));
    }

    private void sendWelcomeMessage(Member member, Guild guild, byte[] image) {
        TextChannel ch = guild.getTextChannelById(welcomeChannelId);
        if (ch == null) return;

        String header = String.format("### - WELCOME TO HIGHCORE MC | %s", member.getAsMention());
        String guide = """
                **Start Here :**

                Highcore → <#1487138386258165820>
                Verification → <#1488279212786843850>
                Rules → <#1487138587827900486>
                """;

        String bannerUrl = (image != null) ? "attachment://welcome.png" : EmbedUtil.BANNER_WELCOME;
        List<net.dv8tion.jda.api.components.container.ContainerChildComponent> layout = new ArrayList<>();
        if (bannerUrl != null)
            layout.add(net.dv8tion.jda.api.components.mediagallery.MediaGallery
                    .of(net.dv8tion.jda.api.components.mediagallery.MediaGalleryItem.fromUrl(bannerUrl)));
        layout.add(net.dv8tion.jda.api.components.textdisplay.TextDisplay.of(header));
        layout.add(net.dv8tion.jda.api.components.separator.Separator
                .createDivider(net.dv8tion.jda.api.components.separator.Separator.Spacing.SMALL));
        layout.add(net.dv8tion.jda.api.components.textdisplay.TextDisplay.of(guide));
        Container c = Container.of(layout);

        var message = ch.sendMessageComponents(c).useComponentsV2(true);
        if (image != null) {
            message.addFiles(FileUpload.fromData(image, "welcome.png"));
        }
        message.queue();
    }

    private void sendStartupDM(Member member, byte[] image) {
        String header = String.format("### - WELCOME TO HIGHCORE MC | %s", member.getUser().getAsMention());
        String guide = """
                **Start Here :**

                Highcore → <#1487138386258165820>
                Verification → <#1488279212786843850>
                Rules → <#1487138587827900486>
                """;

        String bannerUrl = (image != null) ? "attachment://welcome.png" : EmbedUtil.BANNER_MAIN;
        List<net.dv8tion.jda.api.components.container.ContainerChildComponent> layout = new ArrayList<>();
        if (bannerUrl != null)
            layout.add(net.dv8tion.jda.api.components.mediagallery.MediaGallery
                    .of(net.dv8tion.jda.api.components.mediagallery.MediaGalleryItem.fromUrl(bannerUrl)));
        layout.add(net.dv8tion.jda.api.components.textdisplay.TextDisplay.of(header));
        layout.add(net.dv8tion.jda.api.components.separator.Separator
                .createDivider(net.dv8tion.jda.api.components.separator.Separator.Spacing.SMALL));
        layout.add(net.dv8tion.jda.api.components.textdisplay.TextDisplay.of(guide));
        Container c = Container.of(layout);

        member.getUser().openPrivateChannel().queue(
                dm -> {
                    var msg = dm.sendMessageComponents(c).useComponentsV2(true);
                    if (image != null) {
                        msg.addFiles(FileUpload.fromData(image, "welcome.png"));
                    }
                    msg.queue(null, err -> {});
                },
                err -> {});
    }
}
