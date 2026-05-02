package com.integrafty.opexy.listener;

import com.integrafty.opexy.entity.VoiceRoomEntity;
import com.integrafty.opexy.repository.VoiceRoomRepository;
import com.integrafty.opexy.utils.EmbedUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.modals.Modal;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class VoiceListener extends ListenerAdapter {

    private final JDA jda;
    private final VoiceRoomRepository voiceRoomRepository;

    // TODO: Move these to database/config
    private static final String JOIN_TO_CREATE_ID = "1500190617353851037";
    private static final String VOICE_CATEGORY_ID = "1486872074369892392";
    private static final String VOICE_DASHBOARD_ID = "1486872077263835157";

    @PostConstruct
    public void init() {
        jda.addEventListener(this);
    }

    @Override
    public void onGuildVoiceUpdate(GuildVoiceUpdateEvent event) {
        Member member = event.getMember();
        
        // Handle Joining "Join to Create"
        if (event.getChannelJoined() != null) {
            String joinedId = event.getChannelJoined().getId();
            String joinedName = event.getChannelJoined().getName();
            log.info("🔊 {} joined: {} ({})", member.getEffectiveName(), joinedName, joinedId);
            
            boolean isJoinToCreate = joinedId.equals(JOIN_TO_CREATE_ID) || joinedName.toLowerCase().contains("join to create");
            
            if (isJoinToCreate) {
                log.info("✨ Triggering Room Creation for {}", member.getEffectiveName());
                handleCreateRoom(member, event.getChannelJoined().asVoiceChannel().getParentCategory());
            }
        }

        // Handle Leaving Room (Delete if empty)
        if (event.getChannelLeft() != null) {
            VoiceChannel leftChannel = event.getChannelLeft().asVoiceChannel();
            String leftChannelId = leftChannel.getId();
            String leftName = leftChannel.getName();
            String parentId = leftChannel.getParentCategoryId();
            
            log.info("🚪 {} left channel: {} ({}) (Parent: {})", member.getEffectiveName(), leftName, leftChannelId, parentId);

            // CRITICAL: Never delete the Join-to-Create or Dashboard channels
            boolean isJoinToCreate = leftChannelId.equals(JOIN_TO_CREATE_ID) || leftName.toLowerCase().contains("join to create");
            if (isJoinToCreate || leftChannelId.equals(VOICE_DASHBOARD_ID)) {
                log.info("🛡️ Shielding channel from deletion: {}", leftName);
                return;
            }
            
            if (leftChannel.getMembers().isEmpty()) {
                // Check if it's a managed room (by DB or Category)
                Optional<VoiceRoomEntity> roomOpt = voiceRoomRepository.findByChannelId(leftChannelId);
                boolean isInCategory = parentId != null && parentId.equals(VOICE_CATEGORY_ID);
                
                log.info("🔍 Deletion Check for {}: inDB={}, isInCategory={}", leftName, roomOpt.isPresent(), isInCategory);

                if (roomOpt.isPresent() || isInCategory) {
                    log.info("🗑️ Deleting empty managed channel: {}", leftName);
                    
                    if (roomOpt.isPresent()) {
                        VoiceRoomEntity room = roomOpt.get();
                        room.setRoomName(leftName);
                        room.setUserLimit(leftChannel.getUserLimit());
                        room.setBitrate(leftChannel.getBitrate());
                        room.setChannelId(null); // This is now nullable in the entity
                        voiceRoomRepository.save(room);
                    }
                    
                    leftChannel.delete().queue(
                        v -> log.info("✅ Channel deleted successfully"),
                        err -> log.error("❌ Failed to delete channel: {}", err.getMessage())
                    );
                }
            } else {
                log.info("ℹ️ Channel not empty, skipping deletion. (Members: {})", leftChannel.getMembers().size());
            }
        }
    }

    private void handleCreateRoom(Member member, Category category) {
        // Capture existing settings before wipe if any
        List<VoiceRoomEntity> existing = voiceRoomRepository.findAllByOwnerId(member.getId());
        VoiceRoomEntity room = existing.isEmpty() ? new VoiceRoomEntity() : existing.get(0);
        
        // Wipe ALL records for this user (including duplicates) using Native SQL
        voiceRoomRepository.deleteAllByOwnerIdNative(member.getId());
        
        String channelName = room.getRoomName() != null ? room.getRoomName() : "🔊 | " + member.getEffectiveName();
        int userLimit = room.getUserLimit() != null ? room.getUserLimit() : 0;
        
        // Prevent Duplication: Check if user already has a voice channel in this category
        member.getGuild().getVoiceChannels().stream()
            .filter(vc -> vc.getParentCategoryId() != null && vc.getParentCategoryId().equals(category.getId()))
            .filter(vc -> vc.getName().contains(member.getEffectiveName()))
            .forEach(vc -> vc.delete().queue());

        member.getGuild().createVoiceChannel(channelName, category)
            .addPermissionOverride(member.getGuild().getSelfMember(), EnumSet.of(Permission.VIEW_CHANNEL, Permission.MANAGE_CHANNEL), null)
            .addPermissionOverride(member.getGuild().getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL))
            .addPermissionOverride(member, EnumSet.of(Permission.VIEW_CHANNEL, Permission.VOICE_MOVE_OTHERS, Permission.MANAGE_CHANNEL), null)
            .queue(channel -> {
                // Apply voice settings
                if (userLimit > 0) channel.getManager().setUserLimit(userLimit).queue();
                if (room.getBitrate() != null) channel.getManager().setBitrate(room.getBitrate()).queue();

                // Update DB
                room.setOwnerId(member.getId());
                room.setChannelId(channel.getId());
                room.setRoomName(channelName);
                room.setUserLimit(userLimit);
                room.setStatus("OPEN");
                voiceRoomRepository.save(room);

                // Move member
                member.getGuild().moveVoiceMember(member, channel).queue();

                // Send Control Panel
                sendControlPanel(channel, member);
            });
    }

    private void sendControlPanel(VoiceChannel channel, Member owner) {
        String body = "### 🎙️ Voice Control Module\n" +
                "Manage your dynamic frequency, " + owner.getAsMention() + ".\n" +
                "Configurations persist across sessions.";

        ActionRow row1 = ActionRow.of(
            Button.secondary("voice_rename", "Change Name"),
            Button.secondary("voice_limit", "Change Limit"),
            Button.secondary("voice_bitrate", "Change Bitrate")
        );
        
        ActionRow row2 = ActionRow.of(
            Button.success("voice_permit", "Add Member"),
            Button.success("voice_kick", "Kick Member"),
            Button.success("voice_join_perm", "Join Permission")
        );

        ActionRow row3 = ActionRow.of(
            Button.success("voice_speak_perm", "Speak Permission"),
            Button.success("voice_write_perm", "Write Permission"),
            Button.success("voice_video_perm", "Video Permission")
        );

        ActionRow row4 = ActionRow.of(
            Button.primary("voice_region", "Change Region"),
            Button.primary("voice_trust", "Trust Manage"),
            Button.primary("voice_block", "Block Manage")
        );

        ActionRow row5 = ActionRow.of(
            Button.primary("voice_ownership", "Ownership"),
            Button.primary("voice_shop", "Shop"),
            Button.primary("voice_member_panel", "Member Panel")
        );

        ActionRow row6 = ActionRow.of(
            Button.danger("voice_request_staff", "Request Staff"),
            Button.danger("voice_delete", "Delete Channel")
        );

        Container container = EmbedUtil.containerBranded("VOICE", "Management Terminal", body, EmbedUtil.BANNER_MAIN, row1, row2, row3, row4, row5, row6);

        MessageCreateBuilder builder = new MessageCreateBuilder();
        builder.setComponents(container);
        builder.useComponentsV2(true);

        channel.sendMessage(builder.build()).useComponentsV2(true).queue();
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String id = event.getComponentId();
        if (!id.startsWith("voice_")) return;

        VoiceChannel channel = null;
        if (event.getChannelType().isAudio()) {
            channel = event.getChannel().asVoiceChannel();
        } else {
            // Find user's room from repository using their ID as primary key
            Optional<VoiceRoomEntity> ownedRoom = voiceRoomRepository.findById(event.getUser().getId());
            if (ownedRoom.isPresent() && ownedRoom.get().getChannelId() != null) {
                channel = event.getGuild().getVoiceChannelById(ownedRoom.get().getChannelId());
            }
        }

        if (channel == null) {
            event.reply("❌ لا تملك غرفة نشطة للتحكم بها حالياً.").setEphemeral(true).queue();
            return;
        }

        Optional<VoiceRoomEntity> roomOpt = voiceRoomRepository.findByChannelId(channel.getId());
        if (roomOpt.isEmpty()) {
            event.reply("❌ هذه الغرفة ليست مسجلة في النظام كغرفة مؤقتة.").setEphemeral(true).queue();
            return;
        }

        VoiceRoomEntity room = roomOpt.get();
        boolean isOwner = room.getOwnerId().equals(event.getUser().getId());
        boolean isAdmin = event.getMember().hasPermission(Permission.MANAGE_CHANNEL);

        if (!isOwner && !isAdmin && !id.equals("voice_ownership")) {
            event.reply("❌ لا تملك صلاحية للتحكم في هذه الغرفة.").setEphemeral(true).queue();
            return;
        }

        switch (id) {
            case "voice_rename":
                event.replyModal(Modal.create("modal_voice_rename", "تغيير اسم الغرفة")
                    .addComponents(ActionRow.of(TextInput.create("voice_new_name", "الاسم الجديد", TextInputStyle.SHORT).setPlaceholder("🔊 | My Room").build())).build()).queue();
                break;
            case "voice_limit":
                event.replyModal(Modal.create("modal_voice_limit", "حد الأعضاء")
                    .addComponents(ActionRow.of(TextInput.create("voice_new_limit", "العدد (0-99)", TextInputStyle.SHORT).setPlaceholder("0 = لا يوجد حد").build())).build()).queue();
                break;
            case "voice_bitrate":
                StringSelectMenu bitrateMenu = StringSelectMenu.create("menu_voice_bitrate")
                    .setPlaceholder("اختر جودة الصوت (Bitrate)")
                    .addOption("64 kbps (Standard)", "64")
                    .addOption("96 kbps (High)", "96")
                    .addOption("128 kbps (Elite)", "128")
                    .addOption("256 kbps (Premium)", "256")
                    .addOption("384 kbps (Ultra)", "384")
                    .build();
                event.reply("⚙️ يرجى اختيار جودة الصوت المطلوبة:").setEphemeral(true).addComponents(ActionRow.of(bitrateMenu)).queue();
                break;
            case "voice_kick":
                StringSelectMenu.Builder kickMenu = StringSelectMenu.create("menu_voice_kick")
                    .setPlaceholder("اختر العضو المراد طرده");
                
                List<Member> membersInRoom = channel.getMembers().stream()
                    .filter(m -> !m.getUser().isBot())
                    .filter(m -> !m.getId().equals(event.getUser().getId()))
                    .toList();

                if (membersInRoom.isEmpty()) {
                    event.reply("❌ لا يوجد أعضاء آخرون في الغرفة حالياً.").setEphemeral(true).queue();
                    return;
                }

                membersInRoom.forEach(m -> kickMenu.addOption(m.getEffectiveName(), m.getId()));
                event.reply("👞 اختر العضو الذي تريد طرده من الغرفة:").setEphemeral(true).addComponents(ActionRow.of(kickMenu.build())).queue();
                break;
            case "voice_video_perm":
                togglePermission(channel, event, Permission.PRIORITY_SPEAKER, "صلاحية الفيديو/الشير");
                break;
            case "voice_write_perm":
                togglePermission(channel, event, Permission.MESSAGE_SEND, "صلاحية الكتابة");
                break;
            case "voice_speak_perm":
                togglePermission(channel, event, Permission.VOICE_SPEAK, "صلاحية التحدث");
                break;
            case "voice_region":
                StringSelectMenu regionMenu = StringSelectMenu.create("menu_voice_region")
                    .setPlaceholder("اختر منطقة الاتصال")
                    .addOption("Automatic (Recommended)", "auto")
                    .addOption("US East", "us-east")
                    .addOption("US West", "us-west")
                    .addOption("Europe (Rotterdam)", "rotterdam")
                    .addOption("Russia", "russia")
                    .addOption("Singapore", "singapore")
                    .addOption("Japan", "japan")
                    .build();
                event.reply("🌍 اختر المنطقة الجغرافية لخادم الغرفة:").setEphemeral(true).addComponents(ActionRow.of(regionMenu)).queue();
                break;
            case "voice_trust":
                event.replyModal(Modal.create("modal_voice_trust", "إعطاء صلاحية دخول")
                    .addComponents(ActionRow.of(TextInput.create("voice_user_id", "ID العضو", TextInputStyle.SHORT).build())).build()).queue();
                break;
            case "voice_block":
                event.replyModal(Modal.create("modal_voice_block", "حظر عضو من الدخول")
                    .addComponents(ActionRow.of(TextInput.create("voice_user_id", "ID العضو", TextInputStyle.SHORT).build())).build()).queue();
                break;
            case "voice_ownership":
                String ownerMention = event.getGuild().getMemberById(room.getOwnerId()).getAsMention();
                event.reply("👑 المالك الحالي للغرفة هو: " + ownerMention)
                    .setEphemeral(true)
                    .addComponents(ActionRow.of(Button.danger("voice_transfer_start", "نقل الملكية")))
                    .queue();
                break;
            case "voice_transfer_start":
                event.replyModal(Modal.create("modal_voice_transfer", "نقل ملكية الغرفة")
                    .addComponents(ActionRow.of(TextInput.create("voice_user_id", "ID المالك الجديد", TextInputStyle.SHORT).build())).build()).queue();
                break;
            case "voice_panel":
                sendMemberPanel(channel, event);
                break;
            case "voice_request_staff":
                event.reply("🛠️ هل أنت متأكد من رغبتك في فتح تذكرة دعم فني؟")
                    .setEphemeral(true)
                    .addComponents(ActionRow.of(
                        Button.danger("voice_staff_confirm", "تأكيد"),
                        Button.secondary("voice_cancel", "إلغاء")
                    )).queue();
                break;
            case "voice_staff_confirm":
                event.reply("✅ تم فتح التذكرة بنجاح. سيقوم الطاقم بالتواصل معك قريباً.").setEphemeral(true).queue();
                // Logic to open ticket would go here - integrating with TicketListener if needed
                break;
            case "voice_delete":
                event.reply("⚠️ هل أنت متأكد من حذف الغرفة الصوتية بالكامل؟")
                    .setEphemeral(true)
                    .addComponents(ActionRow.of(
                        Button.danger("voice_delete_confirm", "حذف نهائي"),
                        Button.secondary("voice_cancel", "إلغاء")
                    )).queue();
                break;
            case "voice_delete_confirm":
                channel.delete().queue();
                event.reply("🗑️ تم إغلاق التردد وحذف الغرفة.").setEphemeral(true).queue();
                break;
            case "voice_cancel":
                event.reply("❌ تم إلغاء العملية.").setEphemeral(true).queue();
                break;
        }
    }

    private void togglePermission(VoiceChannel channel, ButtonInteractionEvent event, Permission perm, String label) {
        boolean isDenied = channel.getPermissionOverride(event.getGuild().getPublicRole()) != null && 
                           channel.getPermissionOverride(event.getGuild().getPublicRole()).getDenied().contains(perm);
        
        if (isDenied) {
            channel.getManager().putRolePermissionOverride(event.getGuild().getPublicRole().getIdLong(), EnumSet.of(perm), null).queue();
            event.reply("✅ تم **تفعيل** " + label + " للجميع.").setEphemeral(true).queue();
        } else {
            channel.getManager().putRolePermissionOverride(event.getGuild().getPublicRole().getIdLong(), null, EnumSet.of(perm)).queue();
            event.reply("❌ تم **تعطيل** " + label + " للجميع.").setEphemeral(true).queue();
        }
    }

    private void sendMemberPanel(VoiceChannel channel, ButtonInteractionEvent event) {
        StringBuilder sb = new StringBuilder("### 👥 إدارة أفراد الغرفة\n");
        channel.getMembers().forEach(m -> {
            boolean canSpeak = m.hasPermission(channel, Permission.VOICE_SPEAK);
            boolean canWrite = m.hasPermission(channel, Permission.MESSAGE_SEND);
            sb.append("● ").append(m.getAsMention()).append(" — ")
              .append(canSpeak ? "🎙️ " : "")
              .append(canWrite ? "✍️ " : "")
              .append("\n");
        });
        event.reply(sb.toString()).setEphemeral(true).queue();
    }

    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {
        String id = event.getComponentId();
        VoiceChannel channel = event.getMember().getVoiceState().getChannel() != null ? event.getMember().getVoiceState().getChannel().asVoiceChannel() : null;

        if (channel == null) {
            event.reply("❌ يجب أن تكون داخل الغرفة الصوتية لاستخدام القائمة.").setEphemeral(true).queue();
            return;
        }

        String value = event.getValues().get(0);
        switch (id) {
            case "menu_voice_bitrate":
                int bitrate = Integer.parseInt(value) * 1000;
                channel.getManager().setBitrate(bitrate).queue();
                event.reply("✅ تم تحديث جودة الصوت إلى: " + value + " kbps").setEphemeral(true).queue();
                break;
            case "menu_voice_kick":
                event.getGuild().retrieveMemberById(value).queue(m -> {
                    if (channel.getMembers().contains(m)) {
                        event.getGuild().kickVoiceMember(m).queue();
                        event.reply("👞 تم طرد " + m.getAsMention() + " من الغرفة.").setEphemeral(true).queue();
                    }
                });
                break;
            case "menu_voice_region":
                channel.getManager().setRegion(value.equals("auto") ? null : net.dv8tion.jda.api.Region.fromKey(value)).queue();
                event.reply("🌍 تم تغيير منطقة الاتصال إلى: " + value).setEphemeral(true).queue();
                break;
        }
    }

    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        String modalId = event.getModalId();
        if (!modalId.startsWith("modal_voice_")) return;
        
        Optional<VoiceRoomEntity> roomOpt = voiceRoomRepository.findById(event.getUser().getId());
        if (roomOpt.isEmpty() || roomOpt.get().getChannelId() == null) {
            event.reply("❌ لا يمكن العثور على غرفتك الخاصة.").setEphemeral(true).queue();
            return;
        }
        VoiceChannel channel = event.getGuild().getVoiceChannelById(roomOpt.get().getChannelId());
        if (channel == null) {
            event.reply("❌ غرفتك ليست نشطة حالياً.").setEphemeral(true).queue();
            return;
        }

        switch (modalId) {
            case "modal_voice_rename":
                String newName = event.getValue("voice_new_name").getAsString();
                channel.getManager().setName(newName).queue();
                event.reply("✅ تم تحديث اسم الغرفة إلى: " + newName).setEphemeral(true).queue();
                break;
            case "modal_voice_limit":
                try {
                    int limit = Integer.parseInt(event.getValue("voice_new_limit").getAsString());
                    channel.getManager().setUserLimit(limit).queue();
                    event.reply("✅ تم تحديث حد الأعضاء إلى: " + limit).setEphemeral(true).queue();
                } catch (Exception e) { event.reply("❌ يرجى إدخال رقم صحيح.").setEphemeral(true).queue(); }
                break;
            case "modal_voice_trust":
                String trustId = event.getValue("voice_user_id").getAsString();
                event.getGuild().retrieveMemberById(trustId).queue(m -> {
                    channel.getManager().putMemberPermissionOverride(m.getIdLong(), EnumSet.of(Permission.VIEW_CHANNEL, Permission.VOICE_CONNECT), null).queue();
                    event.reply("🤝 تم إعطاء صلاحية الدخول لـ " + m.getAsMention()).setEphemeral(true).queue();
                }, err -> event.reply("❌ لم يتم العثور على العضو.").setEphemeral(true).queue());
                break;
            case "modal_voice_block":
                String blockId = event.getValue("voice_user_id").getAsString();
                event.getGuild().retrieveMemberById(blockId).queue(m -> {
                    channel.getManager().putMemberPermissionOverride(m.getIdLong(), null, EnumSet.of(Permission.VOICE_CONNECT)).queue();
                    if (channel.getMembers().contains(m)) event.getGuild().kickVoiceMember(m).queue();
                    event.reply("🚫 تم حظر " + m.getAsMention() + " من دخول الغرفة.").setEphemeral(true).queue();
                }, err -> event.reply("❌ لم يتم العثور على العضو.").setEphemeral(true).queue());
                break;
            case "modal_voice_transfer":
                String transferId = event.getValue("voice_user_id").getAsString();
                event.getGuild().retrieveMemberById(transferId).queue(m -> {
                    VoiceRoomEntity room = roomOpt.get();
                    room.setOwnerId(m.getId());
                    voiceRoomRepository.save(room);
                    channel.getManager().putMemberPermissionOverride(m.getIdLong(), EnumSet.of(Permission.MANAGE_CHANNEL, Permission.VOICE_MOVE_OTHERS), null).queue();
                    event.reply("👑 تم نقل ملكية الغرفة إلى " + m.getAsMention()).setEphemeral(true).queue();
                }, err -> event.reply("❌ لم يتم العثور على العضو.").setEphemeral(true).queue());
                break;
        }
    }
}
