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
                event.getChannelLeft().delete().queue();
                voiceRoomRepository.deleteByChannelId(leftChannelId);
            }
        }
    }

    private void handleCreateRoom(Member member, Category category) {
        String channelName = "🔊 | " + member.getEffectiveName();
        
        member.getGuild().createVoiceChannel(channelName, category)
            .addPermissionOverride(member.getGuild().getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL))
            .addPermissionOverride(member, EnumSet.of(Permission.VIEW_CHANNEL, Permission.VOICE_MOVE_OTHERS, Permission.MANAGE_CHANNEL), null)
            .queue(channel -> {
                // Save to DB
                VoiceRoomEntity room = new VoiceRoomEntity();
                room.setChannelId(channel.getId());
                room.setOwnerId(member.getId());
                room.setStatus("OPEN");
                voiceRoomRepository.save(room);

                // Move member
                member.getGuild().moveVoiceMember(member, channel).queue();

                // Send Control Panel (V2 Container)
                sendControlPanel(channel, member);
            });
    }

    private void sendControlPanel(VoiceChannel channel, Member owner) {
        String body = "### 🎙️ مركز التحكم في الصوت\n" +
                "مرحباً بك في غرفتك الخاصة، " + owner.getAsMention() + ".\n" +
                "استخدم الأزرار أدناه لإدارة صلاحيات غرفتك وظهورها.";

        ActionRow row1 = ActionRow.of(
            Button.secondary("voice_lock", "قفل"),
            Button.secondary("voice_unlock", "فتح"),
            Button.secondary("voice_hide", "إخفاء"),
            Button.secondary("voice_unhide", "إظهار"),
            Button.secondary("voice_rename", "تغيير الاسم")
        );

        ActionRow row2 = ActionRow.of(
            Button.secondary("voice_limit", "تحديد العدد"),
            Button.secondary("voice_claim", "استلام الملكية"),
            Button.secondary("voice_kick", "طرد"),
            Button.secondary("voice_permit", "سماح"),
            Button.secondary("voice_reject", "رفض")
        );

        ActionRow row3 = ActionRow.of(
            Button.secondary("voice_trust", "توثيق"),
            Button.secondary("voice_untrust", "إلغاء التوثيق"),
            Button.secondary("voice_ghost", "شبح"),
            Button.secondary("voice_unghost", "إلغاء الشبح"),
            Button.secondary("voice_silence", "صمت")
        );

        ActionRow row4 = ActionRow.of(
            Button.secondary("voice_unsilence", "إلغاء الصمت"),
            Button.secondary("voice_transfer", "نقل الملكية")
        );

        Container container = EmbedUtil.containerBranded("الصوت", "إدارة الغرفة", body, EmbedUtil.BANNER_MAIN, row1, row2, row3, row4);

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
            // Find user's room from repository
            Optional<VoiceRoomEntity> ownedRoom = voiceRoomRepository.findByOwnerId(event.getUser().getId())
                    .stream()
                    .filter(r -> event.getGuild().getVoiceChannelById(r.getChannelId()) != null)
                    .findFirst();
            
            if (ownedRoom.isPresent()) {
                channel = event.getGuild().getVoiceChannelById(ownedRoom.get().getChannelId());
            }
        }

        if (channel == null) {
            Container error = EmbedUtil.error("VOICE", "لا تملك غرفة نشطة للتحكم بها حالياً.");
            MessageCreateBuilder errorBuilder = new MessageCreateBuilder();
            errorBuilder.setComponents(error);
            errorBuilder.useComponentsV2(true);
            event.reply(errorBuilder.build()).setEphemeral(true).useComponentsV2(true).queue();
            return;
        }

        Optional<VoiceRoomEntity> roomOpt = voiceRoomRepository.findByChannelId(channel.getId());
        if (roomOpt.isEmpty()) {
            Container error = EmbedUtil.error("VOICE", "هذه الغرفة ليست مسجلة في النظام.");
            MessageCreateBuilder errorBuilder = new MessageCreateBuilder();
            errorBuilder.setComponents(error);
            errorBuilder.useComponentsV2(true);
            event.reply(errorBuilder.build()).setEphemeral(true).useComponentsV2(true).queue();
            return;
        }

        VoiceRoomEntity room = roomOpt.get();
        boolean isOwner = room.getOwnerId().equals(event.getUser().getId());
        boolean isAdmin = event.getMember().hasPermission(Permission.MANAGE_CHANNEL);

        if (!isOwner && !isAdmin && !id.equals("voice_claim")) {
            Container error = EmbedUtil.error("VOICE", "لا تملك صلاحية للتحكم في هذه الغرفة.");
            MessageCreateBuilder errorBuilder = new MessageCreateBuilder();
            errorBuilder.setComponents(error);
            errorBuilder.useComponentsV2(true);
            event.reply(errorBuilder.build()).setEphemeral(true).useComponentsV2(true).queue();
            return;
        }

        switch (id) {
            case "voice_lock":
                channel.getManager().putRolePermissionOverride(event.getGuild().getPublicRole().getIdLong(), null, EnumSet.of(Permission.VOICE_CONNECT)).queue();
                event.reply("🔒 تم قفل الغرفة بنجاح.").setEphemeral(true).queue();
                break;
            case "voice_unlock":
                channel.getManager().putRolePermissionOverride(event.getGuild().getPublicRole().getIdLong(), EnumSet.of(Permission.VOICE_CONNECT), null).queue();
                event.reply("🔓 تم فتح الغرفة بنجاح.").setEphemeral(true).queue();
                break;
            case "voice_hide":
                channel.getManager().putRolePermissionOverride(event.getGuild().getPublicRole().getIdLong(), null, EnumSet.of(Permission.VIEW_CHANNEL)).queue();
                event.reply("👻 تم إخفاء الغرفة بنجاح.").setEphemeral(true).queue();
                break;
            case "voice_unhide":
                channel.getManager().putRolePermissionOverride(event.getGuild().getPublicRole().getIdLong(), EnumSet.of(Permission.VIEW_CHANNEL), null).queue();
                event.reply("👁️ تم إظهار الغرفة بنجاح.").setEphemeral(true).queue();
                break;
            case "voice_rename": {
                TextInput nameInput = TextInput.create("voice_new_name", TextInputStyle.SHORT)
                    .setPlaceholder("مثال: 🔊 | اجتماع خاص")
                    .setMinLength(1)
                    .setMaxLength(30)
                    .build();
                Modal modal = Modal.create("modal_voice_rename", "تغيير اسم الغرفة")
                    .addComponents(Label.of("الاسم الجديد", nameInput))
                    .build();
                event.replyModal(modal).queue();
                break;
            }
            case "voice_limit": {
                TextInput limitInput = TextInput.create("voice_new_limit", TextInputStyle.SHORT)
                    .setPlaceholder("رقم بين 0 و 99 (0 للإلغاء)")
                    .setMinLength(1)
                    .setMaxLength(2)
                    .build();
                Modal modal = Modal.create("modal_voice_limit", "تحديد عدد الأعضاء")
                    .addComponents(Label.of("العدد", limitInput))
                    .build();
                event.replyModal(modal).queue();
                break;
            }
            case "voice_kick": {
                // Implementation for kick usually needs a select menu of members in room
                // For now, respond with info
                event.reply("👞 لطرد شخص، قم بسحبه خارج الغرفة أو استخدم الأمر الخاص (سيتم إضافة قائمة المساعدة قريباً).").setEphemeral(true).queue();
                break;
            }
            case "voice_claim":
                if (channel.getMembers().stream().noneMatch(m -> m.getId().equals(room.getOwnerId()))) {
                    room.setOwnerId(event.getUser().getId());
                    voiceRoomRepository.save(room);
                    event.reply("👑 تم نقل ملكية الغرفة إليك بنجاح.").setEphemeral(true).queue();
                } else {
                    event.reply("❌ صاحب الغرفة متواجد حالياً، لا يمكنك المطالبة بها.").setEphemeral(true).queue();
                }
                break;
            default:
                event.reply("⏳ سيتم تفعيل هذا الزر قريباً.").setEphemeral(true).queue();
                break;
        }
    }

    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        String modalId = event.getModalId();
        VoiceChannel channel = event.getGuild().getVoiceChannels().stream()
            .filter(vc -> voiceRoomRepository.findByChannelId(vc.getId()).isPresent() && 
                          voiceRoomRepository.findByChannelId(vc.getId()).get().getOwnerId().equals(event.getUser().getId()))
            .findFirst().orElse(null);

        if (channel == null) {
            event.reply("❌ لا يمكن العثور على غرفتك الخاصة للتحكم بها.").setEphemeral(true).queue();
            return;
        }

        if (modalId.equals("modal_voice_rename")) {
            String newName = event.getValue("voice_new_name").getAsString();
            channel.getManager().setName(newName).queue();
            event.reply("✅ تم تغيير اسم الغرفة إلى: " + newName).setEphemeral(true).queue();
        } else if (modalId.equals("modal_voice_limit")) {
            try {
                int limit = Integer.parseInt(event.getValue("voice_new_limit").getAsString());
                if (limit < 0 || limit > 99) throw new NumberFormatException();
                channel.getManager().setUserLimit(limit).queue();
                event.reply("✅ تم تحديد عدد الأعضاء بـ: " + (limit == 0 ? "لا محدود" : limit)).setEphemeral(true).queue();
            } catch (NumberFormatException e) {
                event.reply("❌ يرجى إدخال رقم صحيح بين 0 و 99.").setEphemeral(true).queue();
            }
        }
    }
}
