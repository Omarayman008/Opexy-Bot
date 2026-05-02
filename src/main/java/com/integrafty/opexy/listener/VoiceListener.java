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
    private static final String JOIN_TO_CREATE_ID = "1499728555754520667";
    private static final String VOICE_CATEGORY_ID = "1486872074369892392";
    private static final String VOICE_DASHBOARD_ID = "1486872077263835157";

    @PostConstruct
    public void init() {
        jda.addEventListener(this);
    }

    @Override
    public void onGuildVoiceUpdate(GuildVoiceUpdateEvent event) {
        // Handle Joining "Join to Create"
        if (event.getChannelJoined() != null && event.getChannelJoined().getId().equals(JOIN_TO_CREATE_ID)) {
            handleCreateRoom(event.getMember(), event.getChannelJoined().getParentCategory());
        }

        // Handle Leaving Room (Delete if empty)
        if (event.getChannelLeft() != null) {
            String leftChannelId = event.getChannelLeft().getId();
            Optional<VoiceRoomEntity> roomOpt = voiceRoomRepository.findByChannelId(leftChannelId);
            if (roomOpt.isPresent() && event.getChannelLeft().getMembers().isEmpty()) {
                VoiceRoomEntity room = roomOpt.get();
                // Save current state before deletion
                room.setRoomName(event.getChannelLeft().getName());
                room.setUserLimit(event.getChannelLeft().getUserLimit());
                room.setBitrate(event.getChannelLeft().getBitrate());
                room.setChannelId(null); 
                voiceRoomRepository.save(room);
                
                event.getChannelLeft().delete().queue();
            }
        }
    }

    private void handleCreateRoom(Member member, Category category) {
        // Load saved profile or defaults
        VoiceRoomEntity room = voiceRoomRepository.findById(member.getId())
                .orElse(new VoiceRoomEntity());
        
        String channelName = room.getRoomName() != null ? room.getRoomName() : "🔊 | " + member.getEffectiveName();
        int userLimit = room.getUserLimit() != null ? room.getUserLimit() : 0;
        
        member.getGuild().createVoiceChannel(channelName, category)
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
                event.replyModal(Modal.create("modal_voice_rename", "Change Name").addComponents(Label.of("New Name", TextInput.create("voice_new_name", TextInputStyle.SHORT).build())).build()).queue();
                break;
            case "voice_limit":
                event.replyModal(Modal.create("modal_voice_limit", "Change Limit").addComponents(Label.of("Limit (0-99)", TextInput.create("voice_new_limit", TextInputStyle.SHORT).build())).build()).queue();
                break;
            case "voice_bitrate":
                event.replyModal(Modal.create("modal_voice_bitrate", "Change Bitrate").addComponents(Label.of("Bitrate (kbps)", TextInput.create("voice_new_bitrate", TextInputStyle.SHORT).build())).build()).queue();
                break;
            case "voice_permit":
                event.replyModal(Modal.create("modal_voice_permit", "Add Member").addComponents(Label.of("Member ID", TextInput.create("voice_user_id", TextInputStyle.SHORT).build())).build()).queue();
                break;
            case "voice_kick":
                event.replyModal(Modal.create("modal_voice_kick", "Kick Member").addComponents(Label.of("Member ID", TextInput.create("voice_user_id", TextInputStyle.SHORT).build())).build()).queue();
                break;
            case "voice_join_perm":
                boolean canJoin = channel.getPermissionOverride(event.getGuild().getPublicRole()) != null && channel.getPermissionOverride(event.getGuild().getPublicRole()).getAllowed().contains(Permission.VOICE_CONNECT);
                channel.getManager().putRolePermissionOverride(event.getGuild().getPublicRole().getIdLong(), canJoin ? null : EnumSet.of(Permission.VOICE_CONNECT), canJoin ? EnumSet.of(Permission.VOICE_CONNECT) : null).queue();
                event.reply("🚪 Join permission " + (canJoin ? "DISABLED" : "ENABLED")).setEphemeral(true).queue();
                break;
            case "voice_speak_perm":
                boolean canSpeak = channel.getPermissionOverride(event.getGuild().getPublicRole()) == null || !channel.getPermissionOverride(event.getGuild().getPublicRole()).getDenied().contains(Permission.VOICE_SPEAK);
                channel.getManager().putRolePermissionOverride(event.getGuild().getPublicRole().getIdLong(), !canSpeak ? EnumSet.of(Permission.VOICE_SPEAK) : null, canSpeak ? EnumSet.of(Permission.VOICE_SPEAK) : null).queue();
                event.reply("🎙️ Speak permission " + (canSpeak ? "DISABLED" : "ENABLED")).setEphemeral(true).queue();
                break;
            case "voice_write_perm":
                boolean canWrite = channel.getPermissionOverride(event.getGuild().getPublicRole()) == null || !channel.getPermissionOverride(event.getGuild().getPublicRole()).getDenied().contains(Permission.MESSAGE_SEND);
                channel.getManager().putRolePermissionOverride(event.getGuild().getPublicRole().getIdLong(), !canWrite ? EnumSet.of(Permission.MESSAGE_SEND) : null, canWrite ? EnumSet.of(Permission.MESSAGE_SEND) : null).queue();
                event.reply("✍️ Write permission " + (canWrite ? "DISABLED" : "ENABLED")).setEphemeral(true).queue();
                break;
            case "voice_video_perm":
                boolean canVideo = channel.getPermissionOverride(event.getGuild().getPublicRole()) == null || !channel.getPermissionOverride(event.getGuild().getPublicRole()).getDenied().contains(Permission.PRIORITY_SPEAKER);
                channel.getManager().putRolePermissionOverride(event.getGuild().getPublicRole().getIdLong(), !canVideo ? EnumSet.of(Permission.PRIORITY_SPEAKER) : null, canVideo ? EnumSet.of(Permission.PRIORITY_SPEAKER) : null).queue();
                event.reply("📹 Video/Priority permission " + (canVideo ? "DISABLED" : "ENABLED")).setEphemeral(true).queue();
                break;
            case "voice_region":
                event.replyModal(Modal.create("modal_voice_region", "Change Region").addComponents(Label.of("Region ID (e.g. us-east)", TextInput.create("voice_new_region", TextInputStyle.SHORT).build())).build()).queue();
                break;
            case "voice_trust":
                event.replyModal(Modal.create("modal_voice_trust", "Trust Manage").addComponents(Label.of("Member ID", TextInput.create("voice_user_id", TextInputStyle.SHORT).build())).build()).queue();
                break;
            case "voice_block":
                event.replyModal(Modal.create("modal_voice_block", "Block Manage").addComponents(Label.of("Member ID", TextInput.create("voice_user_id", TextInputStyle.SHORT).build())).build()).queue();
                break;
            case "voice_ownership":
                if (channel.getMembers().stream().noneMatch(m -> m.getId().equals(room.getOwnerId()))) {
                    room.setOwnerId(event.getUser().getId());
                    voiceRoomRepository.save(room);
                    event.reply("👑 Ownership claimed.").setEphemeral(true).queue();
                } else {
                    event.replyModal(Modal.create("modal_voice_transfer", "Transfer Ownership").addComponents(Label.of("New Owner ID", TextInput.create("voice_user_id", TextInputStyle.SHORT).build())).build()).queue();
                }
                break;
            case "voice_shop":
                event.reply("🛒 The Agency Shop is under maintenance. Merit system required.").setEphemeral(true).queue();
                break;
            case "voice_member_panel":
                String members = channel.getMembers().stream().map(Member::getAsMention).reduce((a, b) -> a + ", " + b).orElse("None");
                event.reply("ℹ️ **Active Personnel:** " + members).setEphemeral(true).queue();
                break;
            case "voice_request_staff":
                event.reply("🎧 Staff notification dispatched. Remain on frequency.").setEphemeral(true).queue();
                break;
            case "voice_delete":
                channel.delete().queue();
                event.reply("🗑️ Frequency terminated.").setEphemeral(true).queue();
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
                event.reply("✅ Name updated: " + newName).setEphemeral(true).queue();
                break;
            case "modal_voice_limit":
                try {
                    int limit = Integer.parseInt(event.getValue("voice_new_limit").getAsString());
                    channel.getManager().setUserLimit(limit).queue();
                    event.reply("✅ Limit updated: " + limit).setEphemeral(true).queue();
                } catch (Exception e) { event.reply("❌ Invalid number.").setEphemeral(true).queue(); }
                break;
            case "modal_voice_bitrate":
                try {
                    int bitrate = Integer.parseInt(event.getValue("voice_new_bitrate").getAsString()) * 1000;
                    channel.getManager().setBitrate(bitrate).queue();
                    event.reply("✅ Bitrate updated.").setEphemeral(true).queue();
                } catch (Exception e) { event.reply("❌ Invalid number.").setEphemeral(true).queue(); }
                break;
            case "modal_voice_permit":
                String permitId = event.getValue("voice_user_id").getAsString();
                event.getGuild().retrieveMemberById(permitId).queue(m -> {
                    channel.getManager().putMemberPermissionOverride(m.getIdLong(), EnumSet.of(Permission.VOICE_CONNECT), null).queue();
                    event.reply("✅ " + m.getAsMention() + " permitted.").setEphemeral(true).queue();
                }, err -> event.reply("❌ User not found.").setEphemeral(true).queue());
                break;
            case "modal_voice_kick":
                String kickId = event.getValue("voice_user_id").getAsString();
                event.getGuild().retrieveMemberById(kickId).queue(m -> {
                    if (channel.getMembers().contains(m)) event.getGuild().kickVoiceMember(m).queue();
                    event.reply("👞 " + m.getAsMention() + " kicked.").setEphemeral(true).queue();
                }, err -> event.reply("❌ User not found.").setEphemeral(true).queue());
                break;
            case "modal_voice_trust":
                String trustId = event.getValue("voice_user_id").getAsString();
                event.getGuild().retrieveMemberById(trustId).queue(m -> {
                    channel.getManager().putMemberPermissionOverride(m.getIdLong(), EnumSet.of(Permission.VIEW_CHANNEL, Permission.VOICE_CONNECT), null).queue();
                    event.reply("🤝 " + m.getAsMention() + " trusted.").setEphemeral(true).queue();
                }, err -> event.reply("❌ User not found.").setEphemeral(true).queue());
                break;
            case "modal_voice_block":
                String blockId = event.getValue("voice_user_id").getAsString();
                event.getGuild().retrieveMemberById(blockId).queue(m -> {
                    channel.getManager().putMemberPermissionOverride(m.getIdLong(), null, EnumSet.of(Permission.VOICE_CONNECT)).queue();
                    if (channel.getMembers().contains(m)) event.getGuild().kickVoiceMember(m).queue();
                    event.reply("🚫 " + m.getAsMention() + " blocked.").setEphemeral(true).queue();
                }, err -> event.reply("❌ User not found.").setEphemeral(true).queue());
                break;
            case "modal_voice_transfer":
                String transferId = event.getValue("voice_user_id").getAsString();
                event.getGuild().retrieveMemberById(transferId).queue(m -> {
                    VoiceRoomEntity room = roomOpt.get();
                    room.setOwnerId(m.getId());
                    voiceRoomRepository.save(room);
                    channel.getManager().putMemberPermissionOverride(m.getIdLong(), EnumSet.of(Permission.MANAGE_CHANNEL, Permission.VOICE_MOVE_OTHERS), null).queue();
                    event.reply("👑 Ownership transferred to " + m.getAsMention()).setEphemeral(true).queue();
                }, err -> event.reply("❌ User not found.").setEphemeral(true).queue());
                break;
        }
    }
}
