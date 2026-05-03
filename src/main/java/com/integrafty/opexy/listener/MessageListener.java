package com.integrafty.opexy.listener;

import com.integrafty.opexy.service.AutoReplyService;
import com.integrafty.opexy.service.WordFilterService;
import com.integrafty.opexy.utils.EmbedUtil;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class MessageListener extends ListenerAdapter {

    private final JDA jda;
    private final AutoReplyService autoReplyService;
    private final WordFilterService wordFilterService;

    @Value("${opexy.roles.op-staff}")
    private String staffRoleId;

    @Value("${opexy.channels.mod-log:}")
    private String modLogChannelId;

    @PostConstruct
    public void init() {
        jda.addEventListener(this);
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (!event.isFromGuild()) return;
        if (event.getAuthor().isBot()) return;

        String content = event.getMessage().getContentRaw();

        // Staff bypass word filter
        boolean isStaff = event.getMember() != null &&
                event.getMember().getRoles().stream().anyMatch(r -> r.getId().equals(staffRoleId));

        // Word Filter
        String forbidden = isStaff ? null : wordFilterService.findForbiddenWord(content);
        if (forbidden != null) {
            // 1. Delete message
            event.getMessage().delete().queue(null, err -> {});

            // 2. Alert user (auto-delete after 5s)
            event.getChannel()
                    .sendMessage("⚠️ <@" + event.getAuthor().getId() + ">, your message was removed for containing a restricted word.")
                    .delay(5, java.util.concurrent.TimeUnit.SECONDS)
                    .flatMap(net.dv8tion.jda.api.entities.Message::delete)
                    .queue(null, err -> {});

            // 3. Log to mod-log channel
            if (modLogChannelId != null && !modLogChannelId.isEmpty()) {
                TextChannel logChannel = event.getGuild().getTextChannelById(modLogChannelId);
                if (logChannel != null) {
                    java.time.format.DateTimeFormatter dtf =
                            java.time.format.DateTimeFormatter.ofPattern("MMMM dd, yyyy - HH:mm");
                    String now = java.time.LocalDateTime.now().format(dtf);

                    String logBody = "### 🛡️ RESTRICTED WORD DETECTED\n" +
                            "**User:** <@" + event.getAuthor().getId() + ">\n" +
                            "**Channel:** " + event.getChannel().getAsMention() + "\n" +
                            "**Timestamp:** `" + now + "`\n" +
                            "**Forbidden term:** `" + forbidden + "`\n" +
                            "**Original content:**\n> " + content;

                    MessageCreateBuilder builder = new MessageCreateBuilder();
                    builder.setComponents(EmbedUtil.activityLog("WORD FILTER", logBody, EmbedUtil.DANGER));
                    builder.useComponentsV2(true);
                    logChannel.sendMessage(builder.build()).useComponentsV2(true).queue();
                }
            }
            return;
        }

        // Auto Replies
        String autoReply = autoReplyService.getResponse(content);
        if (autoReply != null) {
            event.getMessage().reply(autoReply).queue();
        }
    }
}
