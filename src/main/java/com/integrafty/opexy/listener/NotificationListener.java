package com.integrafty.opexy.listener;

import com.integrafty.opexy.entity.NotificationEntity;
import com.integrafty.opexy.repository.NotificationRepository;
import com.integrafty.opexy.utils.EmbedUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.modals.Modal;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.JDA;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationListener extends ListenerAdapter {

    private final JDA jda;
    private final NotificationRepository notificationRepository;

    @PostConstruct
    public void init() {
        jda.addEventListener(this);
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String id = event.getComponentId();
        if (!id.startsWith("notify_")) return;

        switch (id) {
            case "notify_add_live":
                event.replyModal(Modal.create("modal_notify_add_live", "Add Live Channel")
                    .addComponents(
                        Label.of("Platform (KICK or TWITCH)", TextInput.create("platform", TextInputStyle.SHORT).setPlaceholder("KICK").setRequired(true).build()),
                        Label.of("Channel Username/ID", TextInput.create("channel_id", TextInputStyle.SHORT).setPlaceholder("shroud").setRequired(true).build()),
                        Label.of("Display Name", TextInput.create("display_name", TextInputStyle.SHORT).setPlaceholder("Shroud").setRequired(true).build())
                    ).build()).queue();
                break;
            case "notify_add_video":
                event.replyModal(Modal.create("modal_notify_add_video", "Add YouTube Channel")
                    .addComponents(
                        Label.of("YouTube Channel ID", TextInput.create("channel_id", TextInputStyle.SHORT).setPlaceholder("UC...").setRequired(true).build()),
                        Label.of("Display Name", TextInput.create("display_name", TextInputStyle.SHORT).setPlaceholder("MrBeast").setRequired(true).build())
                    ).build()).queue();
                break;
            case "notify_remove_live":
                handleRemoveMenu(event, false);
                break;
            case "notify_remove_video":
                handleRemoveMenu(event, true);
                break;
        }
    }

    private void handleRemoveMenu(ButtonInteractionEvent event, boolean isVideo) {
        List<NotificationEntity> list = notificationRepository.findAll().stream()
                .filter(e -> isVideo ? e.getPlatform().equals("YOUTUBE") : !e.getPlatform().equals("YOUTUBE"))
                .toList();

        if (list.isEmpty()) {
            event.reply("No channels found to remove.").setEphemeral(true).queue();
            return;
        }

        StringSelectMenu.Builder menu = StringSelectMenu.create("menu_notify_remove")
                .setPlaceholder("Select channel to remove");

        for (NotificationEntity e : list) {
            menu.addOption(e.getDisplayName() + " (" + e.getPlatform() + ")", String.valueOf(e.getId()));
        }

        event.reply("Select the channel you want to stop tracking:").setEphemeral(true).addComponents(ActionRow.of(menu.build())).queue();
    }

    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        String id = event.getModalId();
        if (!id.startsWith("modal_notify_")) return;

        NotificationEntity entity = new NotificationEntity();
        entity.setGuildId(event.getGuild().getId());

        if (id.equals("modal_notify_add_live")) {
            entity.setPlatform(event.getValue("platform").getAsString().toUpperCase());
            entity.setChannelId(event.getValue("channel_id").getAsString());
            entity.setDisplayName(event.getValue("display_name").getAsString());
        } else if (id.equals("modal_notify_add_video")) {
            entity.setPlatform("YOUTUBE");
            entity.setChannelId(event.getValue("channel_id").getAsString());
            entity.setDisplayName(event.getValue("display_name").getAsString());
        }

        notificationRepository.save(entity);
        event.reply("Successfully added **" + entity.getDisplayName() + "** to tracking!").setEphemeral(true).queue();
    }

    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {
        if (!event.getComponentId().equals("menu_notify_remove")) return;

        try {
            Long id = Long.parseLong(event.getValues().get(0));
            notificationRepository.deleteById(id);
            event.reply("Channel removed from tracking.").setEphemeral(true).queue();
        } catch (Exception e) {
            event.reply("Error removing channel.").setEphemeral(true).queue();
        }
    }
}
