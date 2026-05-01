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

import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
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

    private static final String TICKET_CATEGORY_ID = "1487143174567628840";
    private static final String TRANSCRIPT_CHANNEL_ID = "1487147026427940955";

    @PostConstruct
    public void init() {
        jda.addEventListener(this);
    }

    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {
        if (event.getComponentId().equals("ticket_manage_menu")) {
            handleManageMenu(event);
        }
    }

    private void handleManageMenu(StringSelectInteractionEvent event) {
        String selected = event.getValues().get(0);
        switch (selected) {
            case "ticket_manage_add" -> {
                Modal modal = Modal.create("modal_ticket_add", "إضافة عضو")
                    .addComponents(Label.of("ID العضو", TextInput.create("user_id", TextInputStyle.SHORT).setPlaceholder("اكتب الأيدي الخاص بالعضو هنا").build()))
                    .build();
                event.replyModal(modal).queue();
            }
            case "ticket_manage_remove" -> {
                Modal modal = Modal.create("modal_ticket_remove", "إزالة عضو")
                    .addComponents(Label.of("ID العضو", TextInput.create("user_id", TextInputStyle.SHORT).setPlaceholder("اكتب الأيدي الخاص بالعضو هنا").build()))
                    .build();
                event.replyModal(modal).queue();
            }
            case "ticket_manage_rename" -> {
                Modal modal = Modal.create("modal_ticket_rename", "تغيير اسم التذكرة")
                    .addComponents(Label.of("الاسم الجديد", TextInput.create("new_name", TextInputStyle.SHORT).setPlaceholder("مثال: support-vip").build()))
                    .build();
                event.replyModal(modal).queue();
            }
        }
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String buttonId = event.getComponentId();

        if (buttonId.startsWith("ticket_") && !buttonId.equals("ticket_close") && !buttonId.equals("ticket_claim") && !buttonId.equals("ticket_unclaim")) {
            handleTicketModal(event, buttonId);
        } else if (buttonId.equals("ticket_close")) {
            handleTicketClose(event);
        } else if (buttonId.equals("ticket_claim")) {
            handleClaim(event);
        } else if (buttonId.equals("ticket_unclaim")) {
            handleUnclaim(event);
        }
    }

    private void handleTicketModal(ButtonInteractionEvent event, String buttonId) {
        String categoryId = buttonId.replace("ticket_", "");
        String title = switch (categoryId) {
            case "support" -> "الدعم الفني";
            case "complaint" -> "الشكاوى";
            case "admin_app" -> "التقديم على الإدارة";
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
        String modalId = event.getModalId();
        if (modalId.startsWith("modal_ticket_")) {
            if (modalId.equals("modal_ticket_add")) {
                handleAddMember(event);
            } else if (modalId.equals("modal_ticket_remove")) {
                handleRemoveMember(event);
            } else if (modalId.equals("modal_ticket_rename")) {
                handleRenameTicket(event);
            } else {
                handleTicketCreationFromModal(event);
            }
        }
    }

    private void handleAddMember(ModalInteractionEvent event) {
        String userId = event.getValue("user_id").getAsString();
        event.getGuild().retrieveMemberById(userId).queue(member -> {
            event.getChannel().asTextChannel().getManager().putPermissionOverride(member, java.util.EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND), null).queue();
            event.reply("✅ تم إضافة " + member.getAsMention() + " إلى التذكرة.").queue();
        }, error -> event.reply("❌ لم يتم العثور على عضو بهذا الأيدي.").setEphemeral(true).queue());
    }

    private void handleRemoveMember(ModalInteractionEvent event) {
        String userId = event.getValue("user_id").getAsString();
        event.getGuild().retrieveMemberById(userId).queue(member -> {
            event.getChannel().asTextChannel().getManager().putPermissionOverride(member, null, java.util.EnumSet.of(Permission.VIEW_CHANNEL)).queue();
            event.reply("❌ تم إزالة " + member.getAsMention() + " من التذكرة.").queue();
        }, error -> event.reply("❌ لم يتم العثور على عضو بهذا الأيدي.").setEphemeral(true).queue());
    }

    private void handleRenameTicket(ModalInteractionEvent event) {
        String newName = event.getValue("new_name").getAsString();
        event.getChannel().asTextChannel().getManager().setName(newName).queue();
        event.reply("✅ تم تغيير اسم التذكرة إلى: " + newName).queue();
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
        String categoryId = buttonId.replace("ticket_", ""); // support, complaint, admin_app
        
        switch (categoryId) {
            case "support": 
                categoryName = "Support"; 
                break;
            case "complaint": 
                categoryName = "Complaint"; 
                break;
            case "admin_app": 
                categoryName = "Admin"; 
                break;
        }

        Integer lastNum = ticketRepository.findMaxTicketNumberByCategory(categoryId);
        int nextNum = (lastNum == null) ? 1 : lastNum + 1;
        String formattedNum = String.format("%03d", nextNum);

        Guild guild = event.getGuild();
        Member member = event.getMember();
        String channelName = categoryName + "-" + formattedNum;

        final String finalCategoryName = categoryName;
        final int finalNextNum = nextNum;

        // Create Text Channel in specified Category
        guild.createTextChannel(channelName)
            .setParent(guild.getCategoryById(TICKET_CATEGORY_ID))
            .addPermissionOverride(guild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL))
            .addPermissionOverride(member, EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND), null)
            // Note: Admin roles are automatically granted access by Discord if they have Admin permission
            .queue(channel -> {
                // Save to DB
                TicketEntity ticket = new TicketEntity();
                ticket.setUserId(userId);
                ticket.setChannelId(channel.getId());
                ticket.setCategory(categoryId);
                ticket.setTicketNumber(finalNextNum);
                ticketRepository.save(ticket);

                // Premium V2 Container for Ticket Welcome Message
                String ticketBody = "مرحباً بك " + member.getAsMention() + ".\n\n**تفاصيل الطلب:**\n```\n" + issueDescription + "\n```\n\nفريقنا سيقوم بالرد عليك قريباً.";
                
                Container welcomeContainer = EmbedUtil.containerBranded(
                    "تذكرة جديدة", 
                    "تذكرة " + finalCategoryName, 
                    ticketBody, 
                    EmbedUtil.BANNER_SUPPORT,
                    ActionRow.of(
                        net.dv8tion.jda.api.components.selections.StringSelectMenu.create("ticket_manage_menu")
                            .setPlaceholder("إدارة التذكرة")
                            .addOption("إضافة عضو", "ticket_manage_add")
                            .addOption("إزالة عضو", "ticket_manage_remove")
                            .addOption("تغيير اسم التذكرة", "ticket_manage_rename")
                            .build()
                    ),
                    ActionRow.of(
                        Button.secondary("ticket_claim", "استلام التذكرة"),
                        Button.secondary("ticket_close", "إغلاق التذكرة")
                    )
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

            // Rename channel with -C suffix
            String currentName = channel.getName();
            if (!currentName.endsWith("-c")) {
                channel.getManager().setName(currentName + "-c").queue();
            }
        }

        event.reply("🔒 تم إغلاق التذكرة. سيتم حذف الروم نهائياً خلال دقيقة...").queue(success -> {
            channel.delete().queueAfter(60, java.util.concurrent.TimeUnit.SECONDS);
        });
    }

    private void handleClaim(ButtonInteractionEvent event) {
        if (!event.getMember().hasPermission(Permission.MANAGE_CHANNEL)) {
            event.reply("❌ لا تملك صلاحية لاستلام التذاكر.").setEphemeral(true).queue();
            return;
        }

        TextChannel channel = event.getChannel().asTextChannel();
        Optional<TicketEntity> ticketOpt = ticketRepository.findByChannelId(channel.getId());

        if (ticketOpt.isPresent()) {
            TicketEntity ticket = ticketOpt.get();
            ticket.setStaffId(event.getUser().getId());
            ticketRepository.save(ticket);

            // Update original message with Unclaim button and keep menu
            String ticketBody = "**تم استلام التذكرة بواسطة:** " + event.getMember().getAsMention();
            
            Container claimedContainer = EmbedUtil.containerBranded(
                "تم الاستلام", 
                "نظام التذاكر", 
                ticketBody, 
                EmbedUtil.BANNER_SUPPORT,
                ActionRow.of(
                    net.dv8tion.jda.api.components.selections.StringSelectMenu.create("ticket_manage_menu")
                        .setPlaceholder("إدارة التذكرة")
                        .addOption("إضافة عضو", "ticket_manage_add")
                        .addOption("إزالة عضو", "ticket_manage_remove")
                        .addOption("تغيير اسم التذكرة", "ticket_manage_rename")
                        .build()
                ),
                ActionRow.of(
                    Button.secondary("ticket_unclaim", "إلغاء الاستلام"),
                    Button.secondary("ticket_close", "إغلاق التذكرة")
                )
            );

            // Edit the message that was interacted with
            event.editMessage(new net.dv8tion.jda.api.utils.messages.MessageEditBuilder()
                .setComponents(claimedContainer)
                .useComponentsV2(true)
                .build()).queue();
            
            event.getHook().sendMessage("✅ تم استلام التذكرة.").setEphemeral(true).queue();
        }
    }

    private void handleUnclaim(ButtonInteractionEvent event) {
        TextChannel channel = event.getChannel().asTextChannel();
        Optional<TicketEntity> ticketOpt = ticketRepository.findByChannelId(channel.getId());

        if (ticketOpt.isPresent()) {
            TicketEntity ticket = ticketOpt.get();
            if (!event.getUser().getId().equals(ticket.getStaffId()) && !event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
                event.reply("❌ لا يمكنك إلغاء استلام تذكرة مستلمة من قبل شخص آخر.").setEphemeral(true).queue();
                return;
            }
            
            ticket.setStaffId(null);
            ticketRepository.save(ticket);

            // Restore original welcome container look
            String ticketBody = "مرحباً بك <@" + ticket.getUserId() + ">.\n\nتذكرتك متاحة الآن للاستلام من قبل فريق الدعم.";
            
            Container restoredContainer = EmbedUtil.containerBranded(
                "تذكرة جديدة", 
                "نظام التذاكر", 
                ticketBody, 
                EmbedUtil.BANNER_SUPPORT,
                ActionRow.of(
                    net.dv8tion.jda.api.components.selections.StringSelectMenu.create("ticket_manage_menu")
                        .setPlaceholder("إدارة التذكرة")
                        .addOption("إضافة عضو", "ticket_manage_add")
                        .addOption("إزالة عضو", "ticket_manage_remove")
                        .addOption("تغيير اسم التذكرة", "ticket_manage_rename")
                        .build()
                ),
                ActionRow.of(
                    Button.secondary("ticket_claim", "استلام التذكرة"),
                    Button.secondary("ticket_close", "إغلاق التذكرة")
                )
            );

            event.editMessage(new net.dv8tion.jda.api.utils.messages.MessageEditBuilder()
                .setComponents(restoredContainer)
                .useComponentsV2(true)
                .build()).queue();
            event.getHook().sendMessage("🔓 تم إلغاء استلام التذكرة.").setEphemeral(true).queue();
        }
    }
}
