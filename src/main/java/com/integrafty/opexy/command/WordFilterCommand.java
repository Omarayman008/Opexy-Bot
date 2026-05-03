package com.integrafty.opexy.command;

import com.integrafty.opexy.command.base.SlashCommand;
import com.integrafty.opexy.entity.WordFilterEntity;
import com.integrafty.opexy.repository.WordFilterRepository;
import com.integrafty.opexy.service.WordFilterService;
import com.integrafty.opexy.utils.EmbedUtil;
import org.springframework.beans.factory.annotation.Value;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.modals.Modal;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
import net.dv8tion.jda.api.components.container.Container;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class WordFilterCommand extends ListenerAdapter implements SlashCommand {

    private final JDA jda;
    private final WordFilterService wordFilterService;
    private final WordFilterRepository wordFilterRepository;
    private final LogManager logManager;

    @Value("${opexy.roles.op-staff}")
    private String opStaffRoleId;

    @PostConstruct
    public void init() {
        jda.addEventListener(this);
    }

    @Override
    public String getName() { return "banned-words"; }

    @Override
    public SlashCommandData getCommandData() {
        return Commands.slash("banned-words", "Manage the banned words list");
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        if (!hasAccess(event.getMember())) {
            sendEphemeral(event, EmbedUtil.accessDenied());
            return;
        }
        sendPanel(event, false);
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String id = event.getComponentId();
        if (!hasAccess(event.getMember())) return;

        if (id.equals("bw_add")) {
            TextInput word = TextInput.create("word", TextInputStyle.SHORT)
                    .setPlaceholder("Banned term...").setRequired(true).build();
            event.replyModal(Modal.create("modal_bw_add", "Add Banned Word")
                    .addComponents(net.dv8tion.jda.api.components.label.Label.of("Banned Word", word))
                    .build()).queue();

        } else if (id.equals("bw_remove")) {
            TextInput word = TextInput.create("word", TextInputStyle.SHORT)
                    .setPlaceholder("Word to remove...").setRequired(true).build();
            event.replyModal(Modal.create("modal_bw_remove", "Remove Banned Word")
                    .addComponents(net.dv8tion.jda.api.components.label.Label.of("Remove Word", word))
                    .build()).queue();
        }
    }

    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        String id = event.getModalId();

            String word = event.getValue("word").getAsString();
            wordFilterService.addWord(word);
            sendPanel(event, true);

            // LOGGING
            String logDetails = String.format("### ➕ Banned Word Added\n▫️ **Term:** `%s`\n▫️ **Moderator:** %s",
                    word, event.getMember().getAsMention());
            logManager.logEmbed(event.getGuild(), LogManager.LOG_MODS_CMD, 
                    EmbedUtil.createOldLogEmbed("banned-word-add", logDetails, event.getMember(), null, null, EmbedUtil.SUCCESS));

        } else if (id.equals("modal_bw_remove")) {
            String word = event.getValue("word").getAsString();
            wordFilterService.removeWord(word);
            sendPanel(event, true);

            // LOGGING
            String logDetails = String.format("### ➖ Banned Word Removed\n▫️ **Term:** `%s`\n▫️ **Moderator:** %s",
                    word, event.getMember().getAsMention());
            logManager.logEmbed(event.getGuild(), LogManager.LOG_MODS_CMD, 
                    EmbedUtil.createOldLogEmbed("banned-word-remove", logDetails, event.getMember(), null, null, EmbedUtil.DANGER));
        }
    }

    // Build and send the management panel
    private void sendPanel(Object event, boolean edit) {
        List<WordFilterEntity> all = wordFilterRepository.findAll();
        StringBuilder sb = new StringBuilder("### Banned Terms Registry\n\n");
        if (all.isEmpty()) {
            sb.append("*No terms indexed.*");
        } else {
            all.forEach(e -> sb.append("▫️ `").append(e.getWord()).append("`\n"));
        }

        ActionRow row = ActionRow.of(
                Button.secondary("bw_add", "Add Term"),
                Button.secondary("bw_remove", "Remove Term"));

        Container container = EmbedUtil.containerBranded("MODERATION", "Filter Hub", sb.toString(), null, row);

        if (edit) {
            MessageEditBuilder builder = new MessageEditBuilder().setComponents(container).useComponentsV2(true);
            if (event instanceof ButtonInteractionEvent e)
                e.editMessage(builder.build()).useComponentsV2(true).queue();
            else if (event instanceof ModalInteractionEvent e)
                e.editMessage(builder.build()).useComponentsV2(true).queue();
        } else if (event instanceof SlashCommandInteractionEvent e) {
            MessageCreateBuilder builder = new MessageCreateBuilder().setComponents(container).useComponentsV2(true);
            e.reply(builder.build()).useComponentsV2(true).queue();
        }
    }

    private boolean hasAccess(net.dv8tion.jda.api.entities.Member member) {
        return member != null && member.getRoles().stream().anyMatch(r -> r.getId().equals(opStaffRoleId));
    }

    private void sendEphemeral(SlashCommandInteractionEvent event, Container container) {
        MessageCreateBuilder builder = new MessageCreateBuilder().setComponents(container).useComponentsV2(true);
        event.reply(builder.build()).setEphemeral(true).useComponentsV2(true).queue();
    }
}
