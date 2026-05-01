package com.integrafty.opexy.listener;

import com.integrafty.opexy.entity.TicketEntity;
import com.integrafty.opexy.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.modals.Modal;

import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import com.integrafty.opexy.utils.EmbedUtil;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.JDA;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.awt.Color;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class TicketListener extends ListenerAdapter {

    private final JDA jda;
    private final TicketRepository ticketRepository;

    @PostConstruct
    public void init() {
        jda.addEventListener(this);
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String buttonId = event.getComponentId();

        if (buttonId.startsWith("ticket_") && !buttonId.equals("ticket_close")) {
            handleTicketModal(event, buttonId);
        } else if (buttonId.equals("ticket_close")) {
            handleTicketClose(event);
        }
    }

    private void handleTicketModal(ButtonInteractionEvent event, String buttonId) {
        String categoryId = buttonId.replace("ticket_", "");
        String title = switch (categoryId) {
            case "support" -> "الدعم الفني";
            case "whitelist" -> "تقديم وايت ليست";
            case "hiring" -> "طلب توظيف";
            case "complaint" -> "شكوى";
            default -> "تذكرة جديدة";
        };

        TextInput issueInput = TextInput.create("issue_desc", TextInputStyle.PARAGRAPH)
            .setPlaceholder("الرجاء كتابة تفاصيل موضوعك هنا ليتمكن فريقنا من مساعدتك...")
            .setMinLength(10)
            .setMaxLength(1000)
            .setRequired(true)
            .build();

        Modal modal = Modal.create("modal_" + buttonId, title)
            .addComponents(Label.of("الوصف", issueInput))
            .build();

        event.replyModal(modal).queue();
    }

    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        if (event.getModalId().startsWith("modal_ticket_")) {
            handleTicketCreationFromModal(event);
        }
    }

    private void handleTicketCreationFromModal(ModalInteractionEvent event) {
        String buttonId = event.getModalId().replace("modal_", "");
        String userId = event.getUser().getId();
        String issueDescription = event.getValue("issue_desc").getAsString();
        
        // Prevent creating multiple tickets
        if (ticketRepository.existsByUserIdAndStatus(userId, "OPEN")) {
            event.reply("❌ لديك تذكرة مفتوحة بالفعل! يرجى إغلاقها أولاً.").setEphemeral(true).queue();
            return;
        }

        String categoryName = "";
        String categoryId = buttonId.replace("ticket_", ""); // support, whitelist, etc
        Color embedColor = Color.WHITE;
        
        switch (categoryId) {
            case "support": 
                categoryName = "دعم-فني"; 
                embedColor = Color.decode("#5865F2"); 
                break;
            case "whitelist": 
                categoryName = "وايت-ليست"; 
                embedColor = Color.decode("#57F287"); 
                break;
            case "hiring": 
                categoryName = "توظيف"; 
                embedColor = Color.decode("#4F545C"); 
                break;
            case "complaint": 
                categoryName = "شكوى"; 
                embedColor = Color.decode("#ED4245"); 
                break;
        }

        Guild guild = event.getGuild();
        Member member = event.getMember();
        String channelName = categoryName + "-" + member.getUser().getName();

        final String finalCategoryName = categoryName;
        final Color finalEmbedColor = embedColor;

        // Create Text Channel
        guild.createTextChannel(channelName)
            .addPermissionOverride(guild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL))
            .addPermissionOverride(member, EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND), null)
            // Note: Admin roles are automatically granted access by Discord if they have Admin permission
            .queue(channel -> {
                // Save to DB
                TicketEntity ticket = new TicketEntity();
                ticket.setUserId(userId);
                ticket.setChannelId(channel.getId());
                ticket.setCategory(categoryId);
                ticketRepository.save(ticket);

                // Premium V2 Container for Ticket Welcome Message
                String ticketBody = "مرحباً بك " + member.getAsMention() + ".\n\n**تفاصيل الطلب:**\n```\n" + issueDescription + "\n```\n\nفريقنا سيقوم بالرد عليك قريباً.";
                
                Container welcomeContainer = EmbedUtil.containerBranded(
                    "SESSION", 
                    "تذكرة " + finalCategoryName.replace("-", " "), 
                    ticketBody, 
                    EmbedUtil.BANNER_SUPPORT,
                    ActionRow.of(Button.danger("ticket_close", "🔒 إغلاق التذكرة"))
                );

                MessageCreateBuilder msgBuilder = new MessageCreateBuilder();
                msgBuilder.setComponents(welcomeContainer);
                msgBuilder.useComponentsV2(true);

                // Mention user to ping them in the new channel, then delete the ping msg
                channel.sendMessage(member.getAsMention() + " 👑").queue(msg -> msg.delete().queueAfter(2, java.util.concurrent.TimeUnit.SECONDS));
                
                // Send the main premium message
                channel.sendMessage(msgBuilder.build()).useComponentsV2(true).queue();

                Container success = EmbedUtil.success("TICKETS", "تم إنشاء تذكرتك بنجاح: " + channel.getAsMention());
                MessageCreateBuilder successBuilder = new MessageCreateBuilder();
                successBuilder.setComponents(success);
                successBuilder.useComponentsV2(true);
                event.reply(successBuilder.build()).setEphemeral(true).useComponentsV2(true).queue();
            }, error -> {
                Container errorCont = EmbedUtil.error("ERROR", "حدث خطأ أثناء إنشاء الغرفة، يرجى التأكد من صلاحيات البوت.");
                MessageCreateBuilder errorBuilder = new MessageCreateBuilder();
                errorBuilder.setComponents(errorCont);
                errorBuilder.useComponentsV2(true);
                event.reply(errorBuilder.build()).setEphemeral(true).useComponentsV2(true).queue();
                log.error("Error creating ticket channel", error);
            });
    }

    private void handleTicketClose(ButtonInteractionEvent event) {
        // Allow only admins or ticket creators to close
        boolean isAdmin = event.getMember().hasPermission(Permission.MANAGE_CHANNEL);
        
        TextChannel channel = event.getChannel().asTextChannel();
        Optional<TicketEntity> ticketOpt = ticketRepository.findByChannelId(channel.getId());
        
        if (ticketOpt.isPresent()) {
            TicketEntity ticket = ticketOpt.get();
            if (!isAdmin && !ticket.getUserId().equals(event.getUser().getId())) {
                event.reply("❌ لا تملك صلاحية لإغلاق هذه التذكرة.").setEphemeral(true).queue();
                return;
            }
            ticket.setStatus("CLOSED");
            ticketRepository.save(ticket);
        } else if (!isAdmin) {
             event.reply("❌ لا تملك صلاحية لإغلاق هذه التذكرة.").setEphemeral(true).queue();
             return;
        }

        event.reply("🔒 سيتم إغلاق التذكرة وحذف الغرفة نهائياً خلال 5 ثواني...").queue(success -> {
            channel.delete().queueAfter(5, java.util.concurrent.TimeUnit.SECONDS);
        });
    }
}
