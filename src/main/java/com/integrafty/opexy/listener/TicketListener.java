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
                Modal modal = Modal.create("modal_ticket_add", "إضـافـة عـضـو لـلـتـذكـرة")
                    .addComponents(Label.of("ID الـعـضـو", TextInput.create("user_id", TextInputStyle.SHORT).setPlaceholder("ادخل الأيدي الخاص بالعضو لإضافته...").build()))
                    .build();
                event.replyModal(modal).queue();
            }
            case "ticket_manage_remove" -> {
                Modal modal = Modal.create("modal_ticket_remove", "إزالـة عـضـو مـن الـتـذكـرة")
                    .addComponents(Label.of("ID الـعـضـو", TextInput.create("user_id", TextInputStyle.SHORT).setPlaceholder("ادخل الأيدي الخاص بالعضو لإزالته...").build()))
                    .build();
                event.replyModal(modal).queue();
            }
            case "ticket_manage_rename" -> {
                Modal modal = Modal.create("modal_ticket_rename", "تـغـيـيـر اسـم الـتـذكـرة")
                    .addComponents(Label.of("الاسـم الـجـديـد", TextInput.create("new_name", TextInputStyle.SHORT).setPlaceholder("ادخل الاسم الجديد للتذكرة هنا...").build()))
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
        
        Modal modal = switch (categoryId) {
            case "support" -> Modal.create("modal_ticket_support", "الـدعم الـفـنـي")
                .addComponents(
                    Label.of("نـوع الـدعم (مايـن / دـسكورد)", TextInput.create("support_type", TextInputStyle.SHORT).setPlaceholder("اكتب النوع هنا").build()),
                    Label.of("شـرح الـمـشـكـلـة", TextInput.create("issue", TextInputStyle.PARAGRAPH).setPlaceholder("اكتب تفاصيل المشكلة هنا").build())
                ).build();
            case "complaint" -> Modal.create("modal_ticket_complaint", "الـشـكـاوى")
                .addComponents(
                    Label.of("ID الـشـخـص / Username", TextInput.create("target_user", TextInputStyle.SHORT).setPlaceholder("اكتب بيانات الشخص هنا").build()),
                    Label.of("أيـن (مـايـنـكـرافـت / دـسـكـورد)", TextInput.create("location", TextInputStyle.SHORT).setPlaceholder("اكتب المكان هنا").build())
                ).build();
            case "hire" -> Modal.create("modal_ticket_hire", "الـتـقـديـم عـلـى الإدارة")
                .addComponents(
                    Label.of("الاسـم", TextInput.create("name", TextInputStyle.SHORT).build()),
                    Label.of("الـعـمـر", TextInput.create("age", TextInputStyle.SHORT).build()),
                    Label.of("الـمـهـارات", TextInput.create("skills", TextInputStyle.PARAGRAPH).build()),
                    Label.of("الأقـسـام (Discord, Minecraft, Hype)", TextInput.create("depts", TextInputStyle.SHORT).build())
                ).build();
            default -> null;
        };

        if (modal != null) {
            event.replyModal(modal).queue();
        }
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
            event.reply("✅ تـم إزالـة " + member.getAsMention() + " مـن الـتـذكـرة.").queue();
        }, error -> event.reply("❌ لـم يـتـم الـعـثـور عـلـى عـضـو بـهـذا الأيـدي.").setEphemeral(true).queue());
    }

    private void handleRenameTicket(ModalInteractionEvent event) {
        String newName = event.getValue("new_name").getAsString();
        event.getChannel().asTextChannel().getManager().setName(newName).queue();
        event.reply("✅ تـم تـغـيـيـر اسـم الـتـذكـرة إلـى: " + newName).queue();
    }

    private void handleTicketCreationFromModal(ModalInteractionEvent event) {
        String userId = event.getUser().getId();
        
        // Prevent creating multiple tickets
        if (ticketRepository.existsByUserIdAndStatus(userId, "OPEN")) {
            event.reply("❌ لـديـك تـذكـرة مـفـتـوحـة بـالـفـعـل! يـرجـى إغـلاقـهـا أولاً.").setEphemeral(true).queue();
            return;
        }

        String categoryName = "";
        String categoryId = event.getModalId().replace("modal_ticket_", ""); // support, complaint, hire
        
        switch (categoryId) {
            case "support": 
                categoryName = "support"; 
                break;
            case "complaint": 
                categoryName = "complaint"; 
                break;
            case "hire": 
                categoryName = "Hire"; 
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
        final String finalCategoryId = categoryId;

        // Create Text Channel in specified Category
        guild.createTextChannel(channelName)
            .setParent(guild.getCategoryById(TICKET_CATEGORY_ID))
            .addPermissionOverride(guild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL))
            .addPermissionOverride(member, EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND), null)
            .queue(channel -> {
                // Save to DB
                TicketEntity ticket = new TicketEntity();
                ticket.setUserId(userId);
                ticket.setChannelId(channel.getId());
                ticket.setCategory(finalCategoryId);
                ticket.setTicketNumber(finalNextNum);
                ticketRepository.save(ticket);

                // Build detailed body based on category
                StringBuilder ticketBody = new StringBuilder("Welcome " + member.getAsMention() + " 👋\n\n");
                String pingRole = "@DC Department"; 
                String subject = "";
                String details = "";
                String sector = "";

                if (finalCategoryId.equals("support")) {
                    sector = "SUPPORT CENTER";
                    subject = "Support Request • " + event.getValue("support_type").getAsString();
                    details = "Issue: " + event.getValue("issue").getAsString() + "\nService: Minecraft/Discord";
                } else if (finalCategoryId.equals("complaint")) {
                    sector = "COMPLAINT CENTER";
                    subject = "Complaint Request • " + event.getValue("location").getAsString();
                    details = "Target: " + event.getValue("target_user").getAsString();
                } else if (finalCategoryId.equals("hire")) {
                    sector = "HIRE CENTER";
                    subject = "Staff Application • " + event.getValue("depts").getAsString();
                    details = "Name: " + event.getValue("name").getAsString() + "\nAge: " + event.getValue("age").getAsString();
                }

                ticketBody.append("**Subject:** ").append(subject).append("\n");
                ticketBody.append("**Details:** ").append(details).append("\n\n");
                ticketBody.append("A staff member will be with you shortly — please describe your issue in full detail.");
                
                Container welcomeContainer = EmbedUtil.containerBranded(
                    sector, 
                    "Case #" + finalCategoryName.toUpperCase() + "-" + formattedNum, 
                    ticketBody.toString(), 
                    EmbedUtil.BANNER_SUPPORT,
                    ActionRow.of(
                        net.dv8tion.jda.api.components.selections.StringSelectMenu.create("ticket_manage_menu")
                            .setPlaceholder("إدارة الـتـذكـرة...")
                            .addOption("تـغـيـيـر اسـم الـتـذكـرة", "ticket_manage_rename")
                            .addOption("إضـافـة عـضـو", "ticket_manage_add")
                            .addOption("إزالـة عـضـو", "ticket_manage_remove")
                            .build()
                    ),
                    ActionRow.of(
                        Button.secondary("ticket_claim", "اسـتـلام الـتـذكـرة"),
                        Button.secondary("ticket_close", "إغـلاق الـتـذكـرة")
                    )
                );

                MessageCreateBuilder msgBuilder = new MessageCreateBuilder();
                msgBuilder.setContent(pingRole + " " + member.getAsMention());
                msgBuilder.setComponents(welcomeContainer);
                msgBuilder.useComponentsV2(true);
                
                channel.sendMessage(msgBuilder.build()).useComponentsV2(true).queue();

                Container success = EmbedUtil.success("الإمـدادات", "تـم إنـشـاء تـذكـرتـك بـنـجـاح: " + channel.getAsMention());
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

        event.reply("🔒 تـم إغـلاق الـتـذكـرة. سـيـتـم حـذف الـروم نـهـائـيـاً خـلال دقـيـقـة...").queue(success -> {
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
                "نـظام الـتـذاكـر", 
                "تـم الاسـتـلام", 
                ticketBody, 
                EmbedUtil.BANNER_SUPPORT,
                ActionRow.of(
                    net.dv8tion.jda.api.components.selections.StringSelectMenu.create("ticket_manage_menu")
                        .setPlaceholder("إدارة الـتـذكـرة...")
                        .addOption("تـغـيـيـر اسـم الـتـذكـرة", "ticket_manage_rename")
                        .addOption("إضـافـة عـضـو", "ticket_manage_add")
                        .addOption("إزالـة عـضـو", "ticket_manage_remove")
                        .build()
                ),
                ActionRow.of(
                    Button.secondary("ticket_unclaim", "إلـغـاء الاسـتـلام"),
                    Button.secondary("ticket_close", "إغـلاق الـتـذكـرة")
                )
            );

            // Edit the message that was interacted with
            event.editMessage(new net.dv8tion.jda.api.utils.messages.MessageEditBuilder()
                .setComponents(claimedContainer)
                .useComponentsV2(true)
                .build()).queue();
            
            // Send the specific claim notice from the screenshot
            Container notice = EmbedUtil.containerBranded(
                "NOTICE", 
                "Claimed", 
                "📡 Ticket Handled By: " + event.getMember().getAsMention(), 
                null
            );
            
            MessageCreateBuilder noticeBuilder = new MessageCreateBuilder();
            noticeBuilder.setComponents(notice);
            noticeBuilder.useComponentsV2(true);
            
            channel.sendMessage(noticeBuilder.build()).useComponentsV2(true).queue();
        }
    }

    private void handleUnclaim(ButtonInteractionEvent event) {
        TextChannel channel = event.getChannel().asTextChannel();
        Optional<TicketEntity> ticketOpt = ticketRepository.findByChannelId(channel.getId());

        if (ticketOpt.isPresent()) {
            TicketEntity ticket = ticketOpt.get();
            if (!event.getUser().getId().equals(ticket.getStaffId()) && !event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
                event.reply("❌ لا يـمـكـنـك إلـغـاء اسـتـلام تـذكـرة مـسـتـلـمـة مـن قـبـل شـخـص آخـر.").setEphemeral(true).queue();
                return;
            }
            
            ticket.setStaffId(null);
            ticketRepository.save(ticket);

            // Restore original welcome container look
            String ticketBody = "مرحباً بك <@" + ticket.getUserId() + ">.\n\nتذكرتك متاحة الآن للاستلام من قبل فريق الدعم.";
            
            Container restoredContainer = EmbedUtil.containerBranded(
                "نـظام الـتـذاكـر", 
                "بـانـتـظـار الـرد", 
                ticketBody, 
                EmbedUtil.BANNER_SUPPORT,
                ActionRow.of(
                    net.dv8tion.jda.api.components.selections.StringSelectMenu.create("ticket_manage_menu")
                        .setPlaceholder("إدارة الـتـذكـرة...")
                        .addOption("تـغـيـيـر اسـم الـتـذكـرة", "ticket_manage_rename")
                        .addOption("إضـافـة عـضـو", "ticket_manage_add")
                        .addOption("إزالـة عـضـو", "ticket_manage_remove")
                        .build()
                ),
                ActionRow.of(
                    Button.secondary("ticket_claim", "اسـتـلام الـتـذكـرة"),
                    Button.secondary("ticket_close", "إغـلاق الـتـذكـرة")
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
