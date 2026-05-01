package com.integrafty.opexy.listener;

import com.integrafty.opexy.entity.TicketEntity;
import com.integrafty.opexy.repository.TicketRepository;
import com.integrafty.opexy.service.WhitelistSyncService;
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
    private final WhitelistSyncService whitelistSyncService;

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
        } else if (buttonId.equals("ticket_close_final")) {
            handleFinalClose(event);
        } else if (buttonId.equals("ticket_close_cancel")) {
            event.reply("✅ تـم الـتـراجـع عـن الإغـلاق.").setEphemeral(true).queue();
        } else if (buttonId.equals("ticket_reopen")) {
            handleReopen(event);
        } else if (buttonId.equals("ticket_transcript")) {
            handleTranscript(event);
        } else if (buttonId.equals("ticket_delete_init")) {
            handleDeleteRequest(event);
        } else if (buttonId.equals("ticket_delete_final")) {
            event.getChannel().delete().queue();
        } else if (buttonId.equals("ticket_delete_cancel")) {
            event.reply("✅ تـم الـتـراجـع عـن الـحـذف.").setEphemeral(true).queue();
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
            case "whitelist" -> Modal.create("modal_ticket_whitelist", "الـوايـت لـيـسـت")
                .addComponents(
                    Label.of("Discord User & ID", TextInput.create("discord_info", TextInputStyle.SHORT).setPlaceholder("Example: User#0000 - 123456789...").build()),
                    Label.of("MC Username", TextInput.create("mc_name", TextInputStyle.SHORT).setPlaceholder("اكتب اسمك في اللعبة").build()),
                    Label.of("Version (Bedrok/Java)", TextInput.create("version", TextInputStyle.SHORT).setPlaceholder("Example: Java 1.20.1").build()),
                    Label.of("Type (كراك/اصليه)", TextInput.create("account_type", TextInputStyle.SHORT).setPlaceholder("Example: اصلية").build())
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
        
        boolean isExempt = event.getMember().getRoles().stream().anyMatch(r -> r.getId().equals("1487152572207861870"));
        
        if (!isExempt && ticketRepository.existsByUserIdAndStatus(userId, "OPEN")) {
            event.reply("❌ لـديـك تـذكـرة مـفـتـوحـة بـالـفـعـل! يـرجـى إغـلاقـهـا أولاً.").setEphemeral(true).queue();
            return;
        }

        String categoryName = "";
        String categoryId = event.getModalId().replace("modal_ticket_", ""); 
        
        switch (categoryId) {
            case "support": categoryName = "support"; break;
            case "complaint": categoryName = "complaint"; break;
            case "hire": categoryName = "Hire"; break;
            case "whitelist": categoryName = "Whitelist"; break;
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

        boolean isWhitelist = finalCategoryId.equals("whitelist");
        EnumSet<Permission> memberPerms = isWhitelist ? EnumSet.of(Permission.VIEW_CHANNEL) : EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND);

        guild.createTextChannel(channelName)
            .setParent(guild.getCategoryById(TICKET_CATEGORY_ID))
            .addPermissionOverride(guild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL))
            .addPermissionOverride(member, memberPerms, isWhitelist ? EnumSet.of(Permission.MESSAGE_SEND) : null)
            .queue(channel -> {
                if (isWhitelist) {
                    guild.addRoleToMember(member, guild.getRoleById("1499355941752012900")).queue();
                }
                TicketEntity ticket = new TicketEntity();
                ticket.setUserId(userId);
                ticket.setChannelId(channel.getId());
                ticket.setCategory(finalCategoryId);
                ticket.setTicketNumber(finalNextNum);
                ticketRepository.save(ticket);

                StringBuilder ticketBody = new StringBuilder("Welcome " + member.getAsMention() + " 👋\n\n");
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
                } else if (finalCategoryId.equals("whitelist") || finalCategoryId.equals("modal_ticket_whitelist")) {
                    sector = "WHITELIST CENTER";
                    subject = "Whitelist Request • " + event.getValue("mc_name").getAsString();
                    details = "Discord: " + event.getValue("discord_info").getAsString() + 
                              "\nVersion: " + event.getValue("version").getAsString() + 
                              "\nAccount: " + event.getValue("account_type").getAsString();
                }

                ticketBody.append("**Subject:** ").append(subject).append("\n");
                ticketBody.append("**Details:** ").append(details).append("\n\n");
                
                if (isWhitelist) {
                    ticketBody.append("### ✅ تـم قـبـول طـلـبـك مـبـدئـيـاً\n")
                        .append("يـرجـى اتـبـاع الـخـطـوات الـمـرسـلـة بـالأسـفـل لـتـكـمـلـة الـتـفـعـيـل.");
                } else {
                    ticketBody.append("A staff member will be with you shortly — please describe your issue in full detail.");
                }
                
                Container welcomeContainer;
                if (isWhitelist) {
                    welcomeContainer = EmbedUtil.containerBranded(
                        sector, 
                        "Case #" + finalCategoryName.toUpperCase() + "-" + formattedNum, 
                        ticketBody.toString(), 
                        EmbedUtil.BANNER_SUPPORT
                    );
                } else {
                    welcomeContainer = EmbedUtil.containerBranded(
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
                }

                String ping = "<@&1487152917763981574> " + member.getAsMention();
                channel.sendMessage(ping).queue();
                
                channel.sendMessage(new MessageCreateBuilder().setComponents(welcomeContainer).useComponentsV2(true).build())
                    .useComponentsV2(true)
                    .queue();

                if (isWhitelist) {
                    whitelistSyncService.syncToSupabase(
                        event.getValue("discord_info").getAsString(),
                        event.getValue("mc_name").getAsString(),
                        event.getValue("version").getAsString(),
                        event.getValue("account_type").getAsString()
                    );

                    // Send detailed steps as a separate message
                    String steps = "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬\n" +
                                 "### 📋 خـطـوات الـتـفـعـيـل الـنـهـائـيـة\n\n" +
                                 "**1.** تـم تـسـجـيـل بـيـانـاتـك فـي قـاعـدة بـيـانـات الـسـيـرفـر بـنـجـاح.\n" +
                                 "**2.** تـوجـه الآن إلـى روم الـتـفـعـيـل: <#1488279212786843850>.\n" +
                                 "**3.** قـم بـالـضـغـط عـلـى إيـمـوجـي 🟢 ريـأكـشـن لـتـفـعـيـل حـسـابـك آلياً.\n\n" +
                                 "**مـلاحـظـة:** سـيـتـم تـفـعـيـلـك فـور وضـع الـريـأكـشـن، مـرحـبـاً بـك فـي الـسـيـرفـر!\n" +
                                 "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬";
                    channel.sendMessage(steps).queue();
                }

                Container successCont = EmbedUtil.success("الإنـشـاء", "تـم إنـشـاء تـذكـرتـك بـنـجـاح: " + channel.getAsMention());
                event.reply(new MessageCreateBuilder().setComponents(successCont).useComponentsV2(true).build())
                    .setEphemeral(true)
                    .useComponentsV2(true)
                    .queue();
            }, error -> {
                Container errorCont = EmbedUtil.error("ERROR", "حدث خطأ أثناء إنشاء الغرفة، يرجى التأكد من صلاحيات البوت.");
                event.reply(new MessageCreateBuilder().setComponents(errorCont).useComponentsV2(true).build())
                    .setEphemeral(true).useComponentsV2(true).queue();
                log.error("Error creating ticket channel", error);
            });
    }

    private void handleTicketClose(ButtonInteractionEvent event) {
        Container confirm = EmbedUtil.containerBranded(
            "تـأكـيـد الإغـلاق", 
            "هـل أنـت مـتـأكـد؟", 
            "### هـل أنـت مـتـأكـد مـن رغـبـتـك فـي إغـلاق هـذه الـتـذكـرة؟", 
            EmbedUtil.BANNER_SUPPORT,
            ActionRow.of(
                Button.secondary("ticket_close_final", "تـأكـيـد الإغـلاق"),
                Button.secondary("ticket_close_cancel", "تـراجـع")
            )
        );
        event.reply(new MessageCreateBuilder().setComponents(confirm).useComponentsV2(true).build())
            .setEphemeral(true).queue();
    }

    private void handleFinalClose(ButtonInteractionEvent event) {
        // Use deferReply to give us more time and show we are processing
        event.deferReply(true).queue(hook -> {
            try {
                TextChannel channel = event.getChannel().asTextChannel();
                Member member = event.getMember();
                
                ticketRepository.findByChannelId(channel.getId()).ifPresentOrElse(ticket -> {
                    // 1. Update DB Status
                    ticket.setStatus("CLOSED");
                    ticketRepository.save(ticket);
                    
                    // 2. Remove client write access
                    Member client = event.getGuild().getMemberById(ticket.getUserId());
                    if (client != null) {
                        channel.getManager().putMemberPermissionOverride(client.getIdLong(), 
                            EnumSet.of(Permission.VIEW_CHANNEL), 
                            EnumSet.of(Permission.MESSAGE_SEND)).queue(null, err -> log.warn("Permission override failed: {}", err.getMessage()));
                    }
                    
                    // 3. Rename channel
                    if (!channel.getName().endsWith("-c")) {
                        channel.getManager().setName(channel.getName() + "-c").queue(null, err -> log.warn("Rename failed: {}", err.getMessage()));
                    }
                    
                    // 4. Send Archive Panel
                    Container panel = EmbedUtil.containerBranded(
                        "ARCHIVES", 
                        "لـوحـة الـتـحـكـم", 
                        "### تـم إغـلاق الـتـذكـرة\nبـواسـطـة الـعـضـو **" + member.getEffectiveName() + "**.\n\nاخـتـر إجـراء مـن الأسـفـل.", 
                        EmbedUtil.BANNER_SUPPORT,
                        ActionRow.of(
                            Button.secondary("ticket_reopen", "إعـادة فـتـح"),
                            Button.secondary("ticket_transcript", "سـجـل الـتـحـادث"),
                            Button.secondary("ticket_delete_init", "حـذف الـتـذكـرة")
                        )
                    );
                    
                    channel.sendMessage(new MessageCreateBuilder().setComponents(panel).useComponentsV2(true).build())
                        .useComponentsV2(true).queue();
                    
                    hook.sendMessage("✅ تـم إغـلاق الـتـذكـرة بـنـجـاح.").setEphemeral(true).queue();
                }, () -> {
                    hook.sendMessage("❌ لم يتم العثور على بيانات التذكرة في قاعدة البيانات.").setEphemeral(true).queue();
                });
            } catch (Exception e) {
                log.error("Error in handleFinalClose deferred logic", e);
                hook.sendMessage("❌ حدث خطأ داخلي أثناء إغلاق التذكرة: " + e.getMessage()).setEphemeral(true).queue();
            }
        }, err -> {
            log.error("Failed to defer interaction for final close", err);
        });
    }

    private void handleClaim(ButtonInteractionEvent event) {
        event.deferEdit().queue();
        if (!event.getMember().hasPermission(Permission.MANAGE_CHANNEL)) {
            event.getHook().sendMessage("❌ لا تـمـلـك صـلاحـيـة لاسـتـلام الـتـذاكـر.").setEphemeral(true).queue();
            return;
        }

        TextChannel channel = event.getChannel().asTextChannel();
        Optional<TicketEntity> ticketOpt = ticketRepository.findByChannelId(channel.getId());

        if (ticketOpt.isPresent()) {
            TicketEntity ticket = ticketOpt.get();
            ticket.setStaffId(event.getUser().getId());
            ticketRepository.save(ticket);

            // 1. Rename channel to category-staffname
            String category = channel.getName().split("-")[0];
            String staffName = event.getMember().getEffectiveName().toLowerCase().replace(" ", "");
            channel.getManager().setName(category + "-" + staffName).queue();

            // 2. Update original message buttons
            String ticketBody = "**تـم اسـتـلام الـتـذكـرة بـواسـطـة:** " + event.getMember().getAsMention();
            
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

            event.getHook().editOriginal(new net.dv8tion.jda.api.utils.messages.MessageEditBuilder()
                .setComponents(claimedContainer)
                .useComponentsV2(true)
                .build()).queue();
            
            // 3. Send the specific claim notice
            Container notice = EmbedUtil.containerBranded(
                "NOTICE", 
                "Claimed", 
                "📡 Ticket Handled By: " + event.getMember().getAsMention(), 
                null
            );
            
            channel.sendMessage(new MessageCreateBuilder().setComponents(notice).useComponentsV2(true).build()).useComponentsV2(true).queue();
        }
    }

    private void handleUnclaim(ButtonInteractionEvent event) {
        event.deferEdit().queue();
        TextChannel channel = event.getChannel().asTextChannel();
        Optional<TicketEntity> ticketOpt = ticketRepository.findByChannelId(channel.getId());

        if (ticketOpt.isPresent()) {
            TicketEntity ticket = ticketOpt.get();
            if (!event.getUser().getId().equals(ticket.getStaffId()) && !event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
                event.getHook().sendMessage("❌ لا يـمـكـنـك إلـغـاء اسـتـلام تـذكـرة مـسـتـلـمـة مـن قـبـل شـخـص آخـر.").setEphemeral(true).queue();
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

            event.getHook().editOriginal(new net.dv8tion.jda.api.utils.messages.MessageEditBuilder()
                .setComponents(restoredContainer)
                .useComponentsV2(true)
                .build()).queue();

            // Send unclaim notice
            Container notice = EmbedUtil.containerBranded(
                "NOTICE", 
                "Unclaimed", 
                "⤵️ Ticket Unclaimed By: " + event.getMember().getAsMention(), 
                null
            );
            channel.sendMessage(new MessageCreateBuilder().setComponents(notice).useComponentsV2(true).build())
                .useComponentsV2(true).queue();

            event.getHook().sendMessage("🔓 تم إلغاء استلام التذكرة.").setEphemeral(true).queue();
        }
    }
    private void handleReopen(ButtonInteractionEvent event) {
        event.deferReply(true).queue();
        TextChannel channel = event.getChannel().asTextChannel();
        Optional<TicketEntity> ticketOpt = ticketRepository.findByChannelId(channel.getId());
        if (ticketOpt.isEmpty()) return;
        TicketEntity ticket = ticketOpt.get();

        // 1. Restore client access
        Member client = event.getGuild().getMemberById(ticket.getUserId());
        if (client != null) {
            channel.getManager().putMemberPermissionOverride(client.getIdLong(), 
                EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND), null).queue();
        }

        // 2. Update DB
        ticket.setStatus("OPEN");
        ticketRepository.save(ticket);

        // 3. Rename back (remove -c)
        String currentName = channel.getName();
        if (currentName.endsWith("-c")) {
            channel.getManager().setName(currentName.substring(0, currentName.length() - 2)).queue();
        }

        event.getHook().sendMessage("✅ تـم إعـادة فـتـح الـتـذكـرة وإعـادة الـصـلاحـيـات.").queue();
    }

    private void handleTranscript(ButtonInteractionEvent event) {
        TextChannel channel = event.getChannel().asTextChannel();
        event.deferReply(true).queue();
        
        channel.getIterableHistory().takeAsync(100).thenAccept(messages -> {
            byte[] transcript = com.integrafty.opexy.utils.TranscriptService.generateSimpleTranscript(channel, messages);
            
            // Send to user ephemerally
            event.getHook().sendMessage("📄 سـجـل الـتـحـادث لـلـتـذكـرة: " + channel.getName())
                .addFiles(net.dv8tion.jda.api.utils.FileUpload.fromData(transcript, "transcript-" + channel.getName() + ".txt"))
                .queue();
                
            // Send to logs
            TextChannel logCh = event.getGuild().getTextChannelById("1486872077263835157");
            if (logCh != null) {
                logCh.sendMessage("📄 **TRANSCRIPT** | #" + channel.getName() + " | Closed by " + event.getUser().getAsMention())
                    .addFiles(net.dv8tion.jda.api.utils.FileUpload.fromData(transcript, "transcript-" + channel.getName() + ".txt"))
                    .queue();
            }
        });
    }

    private void handleDeleteRequest(ButtonInteractionEvent event) {
        Container confirm = EmbedUtil.containerBranded(
            "تـحـذيـر", 
            "حـذف الـقـنـاة", 
            "### هـل أنـت مـتـأكـد مـن حـذف الـتـذكـرة نـهـائـيـاً؟\nهـذا الإجـراء لا يـمـكـن الـتـراجـع عـنـه.", 
            EmbedUtil.BANNER_SUPPORT,
            ActionRow.of(
                Button.secondary("ticket_delete_final", "تـأكـيـد الـحـذف"),
                Button.secondary("ticket_delete_cancel", "تـراجـع")
            )
        );
        event.reply(new MessageCreateBuilder().setComponents(confirm).useComponentsV2(true).build())
            .setEphemeral(true).useComponentsV2(true).queue();
    }
}
