package com.integrafty.opexy.command;

import com.integrafty.opexy.command.base.SlashCommand;
import com.integrafty.opexy.entity.NotificationEntity;
import com.integrafty.opexy.repository.NotificationRepository;
import com.integrafty.opexy.utils.EmbedUtil;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.components.container.Container;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class LiveNotifyCommand implements SlashCommand {

    private final NotificationRepository notificationRepository;

    @Override
    public String getName() {
        return "live-notify";
    }

    @Override
    public SlashCommandData getCommandData() {
        return Commands.slash("live-notify", "Manage live stream notifications (Kick/Twitch)");
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        if (!event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
            event.reply("You do not have permission to use this command.").setEphemeral(true).queue();
            return;
        }

        List<NotificationEntity> list = notificationRepository.findAll().stream()
                .filter(e -> !e.getPlatform().equalsIgnoreCase("YOUTUBE"))
                .collect(Collectors.toList());

        StringBuilder sb = new StringBuilder("### Tracked Live Channels\n");
        if (list.isEmpty()) {
            sb.append("*No channels tracked yet.*");
        } else {
            for (NotificationEntity e : list) {
                sb.append(String.format("● **%s** (%s) - `%s`\n", e.getDisplayName(), e.getPlatform(), e.getChannelId()));
            }
        }

        sb.append("\nUse the buttons below to manage your notifications.");

        ActionRow row = ActionRow.of(
            Button.success("notify_add_live", "Add Channel"),
            Button.danger("notify_remove_live", "Remove Channel")
        );

        Container container = EmbedUtil.containerBranded("NOTIFICATIONS", "Live Stream Manager", sb.toString(), EmbedUtil.BANNER_MAIN, row);

        MessageCreateBuilder builder = new MessageCreateBuilder()
            .setComponents(container)
            .useComponentsV2(true);

        event.reply(builder.build()).setEphemeral(true).queue();
    }
}
