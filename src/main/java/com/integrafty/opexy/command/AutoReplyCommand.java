package com.integrafty.opexy.command;

import com.integrafty.opexy.command.base.SlashCommand;
import com.integrafty.opexy.entity.AutoReplyEntity;
import com.integrafty.opexy.repository.AutoReplyRepository;
import com.integrafty.opexy.service.AutoReplyService;
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
public class AutoReplyCommand extends ListenerAdapter implements SlashCommand {

    private final JDA jda;
    private final AutoReplyService autoReplyService;
    private final AutoReplyRepository autoReplyRepository;

    @Value("${opexy.roles.op-staff}")
    private String opStaffRoleId;

    @PostConstruct
    public void init() {
        jda.addEventListener(this);
    }

    @Override
    public String getName() { return "replay"; }

    @Override
    public SlashCommandData getCommandData() {
        return Commands.slash("replay", "Manage auto-reply triggers");
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

        if (id.equals("ar_add")) {
            TextInput trigger = TextInput.create("trigger", TextInputStyle.SHORT)
                    .setPlaceholder("Keyword...").setRequired(true).build();
            TextInput reply = TextInput.create("reply", TextInputStyle.PARAGRAPH)
                    .setPlaceholder("Bot reply...").setRequired(true).build();
            event.replyModal(Modal.create("modal_ar_add", "Add Auto Reply")
                    .addComponents(
                            net.dv8tion.jda.api.components.label.Label.of("Trigger", trigger),
                            net.dv8tion.jda.api.components.label.Label.of("Response", reply))
                    .build()).queue();

        } else if (id.equals("ar_remove")) {
            TextInput trigger = TextInput.create("trigger", TextInputStyle.SHORT)
                    .setPlaceholder("Trigger to remove...").setRequired(true).build();
            event.replyModal(Modal.create("modal_ar_remove", "Remove Auto Reply")
                    .addComponents(net.dv8tion.jda.api.components.label.Label.of("Trigger", trigger))
                    .build()).queue();
        }
    }

    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        String id = event.getModalId();

        if (id.equals("modal_ar_add")) {
            String trigger = event.getValue("trigger").getAsString();
            String reply   = event.getValue("reply").getAsString();
            autoReplyService.addResponse(trigger, reply, event.getUser().getName());
            sendPanel(event, true);

        } else if (id.equals("modal_ar_remove")) {
            String trigger = event.getValue("trigger").getAsString();
            autoReplyService.removeResponse(trigger);
            sendPanel(event, true);
        }
    }

    // Build and send the management panel
    private void sendPanel(Object event, boolean edit) {
        List<AutoReplyEntity> all = autoReplyRepository.findAll();
        StringBuilder sb = new StringBuilder("### Autonomous Response Registry\n\n");
        if (all.isEmpty()) {
            sb.append("*No triggers indexed.*");
        } else {
            all.forEach(e -> sb.append("▫️ **").append(e.getKeyword()).append(":** ").append(e.getResponseText()).append("\n"));
        }

        ActionRow row = ActionRow.of(
                Button.secondary("ar_add", "Add Response"),
                Button.secondary("ar_remove", "Delete Response"));

        Container container = EmbedUtil.containerBranded("AUTOMATION", "Logic Hub", sb.toString(), null, row);

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
