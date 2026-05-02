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
            Button.secondary("voice_lock", "قفل").withEmoji(Emoji.fromUnicode("🔒")),
            Button.secondary("voice_unlock", "فتح").withEmoji(Emoji.fromUnicode("🔓")),
            Button.secondary("voice_rename", "تغيير الاسم").withEmoji(Emoji.fromUnicode("📝"))
        );
        
        ActionRow row2 = ActionRow.of(
            Button.secondary("voice_hide", "إخفاء").withEmoji(Emoji.fromUnicode("👻")),
            Button.secondary("voice_unhide", "إظهار").withEmoji(Emoji.fromUnicode("👁️")),
            Button.secondary("voice_limit", "تحديد العدد").withEmoji(Emoji.fromUnicode("👥"))
        );

        ActionRow row3 = ActionRow.of(
            Button.secondary("voice_claim", "استلام الملكية").withEmoji(Emoji.fromUnicode("👑")),
            Button.secondary("voice_transfer", "نقل الملكية").withEmoji(Emoji.fromUnicode("🔄")),
            Button.secondary("voice_kick", "طرد").withEmoji(Emoji.fromUnicode("👞"))
        );

        ActionRow row4 = ActionRow.of(
            Button.secondary("voice_permit", "سماح").withEmoji(Emoji.fromUnicode("✅")),
            Button.secondary("voice_reject", "رفض").withEmoji(Emoji.fromUnicode("❌")),
            Button.secondary("voice_trust", "توثيق").withEmoji(Emoji.fromUnicode("🤝"))
        );

        ActionRow row5 = ActionRow.of(
            Button.secondary("voice_untrust", "إلغاء التوثيق").withEmoji(Emoji.fromUnicode("🚫")),
            Button.secondary("voice_ghost", "شبح").withEmoji(Emoji.fromUnicode("🎭")),
            Button.secondary("voice_unghost", "إلغاء الشبح").withEmoji(Emoji.fromUnicode("🕶️")),
            Button.secondary("voice_silence", "صمت").withEmoji(Emoji.fromUnicode("🔇")),
            Button.secondary("voice_unsilence", "إلغاء الصمت").withEmoji(Emoji.fromUnicode("🔊"))
        );

        Container container = EmbedUtil.containerBranded("الصوت", "إدارة الغرفة", body, EmbedUtil.BANNER_MAIN, row1, row2, row3, row4, row5);

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
                    .build();
                event.replyModal(Modal.create("modal_voice_rename", "تغيير اسم الغرفة").addComponents(Label.of("الاسم الجديد", nameInput)).build()).queue();
                break;
            }
            case "voice_limit": {
                TextInput limitInput = TextInput.create("voice_new_limit", TextInputStyle.SHORT)
                    .setPlaceholder("رقم بين 0 و 99 (0 للإلغاء)")
                    .build();
                event.replyModal(Modal.create("modal_voice_limit", "تحديد عدد الأعضاء").addComponents(Label.of("العدد", limitInput)).build()).queue();
                break;
            }
            case "voice_kick": {
                TextInput userInput = TextInput.create("voice_user_id", TextInputStyle.SHORT).setPlaceholder("اكتب أيدي الشخص هنا").build();
                event.replyModal(Modal.create("modal_voice_kick", "طرد عضو").addComponents(Label.of("ID العضو", userInput)).build()).queue();
                break;
            }
            case "voice_permit": {
                TextInput userInput = TextInput.create("voice_user_id", TextInputStyle.SHORT).setPlaceholder("اكتب أيدي الشخص هنا").build();
                event.replyModal(Modal.create("modal_voice_permit", "سماح لعضو").addComponents(Label.of("ID العضو", userInput)).build()).queue();
                break;
            }
            case "voice_reject": {
                TextInput userInput = TextInput.create("voice_user_id", TextInputStyle.SHORT).setPlaceholder("اكتب أيدي الشخص هنا").build();
                event.replyModal(Modal.create("modal_voice_reject", "منع عضو").addComponents(Label.of("ID العضو", userInput)).build()).queue();
                break;
            }
            case "voice_trust": {
                TextInput userInput = TextInput.create("voice_user_id", TextInputStyle.SHORT).setPlaceholder("اكتب أيدي الشخص هنا").build();
                event.replyModal(Modal.create("modal_voice_trust", "توثيق عضو").addComponents(Label.of("ID العضو", userInput)).build()).queue();
                break;
            }
            case "voice_untrust": {
                TextInput userInput = TextInput.create("voice_user_id", TextInputStyle.SHORT).setPlaceholder("اكتب أيدي الشخص هنا").build();
                event.replyModal(Modal.create("modal_voice_untrust", "إلغاء توثيق عضو").addComponents(Label.of("ID العضو", userInput)).build()).queue();
                break;
            }
            case "voice_transfer": {
                TextInput userInput = TextInput.create("voice_user_id", TextInputStyle.SHORT).setPlaceholder("اكتب أيدي المالك الجديد هنا").build();
                event.replyModal(Modal.create("modal_voice_transfer", "نقل الملكية").addComponents(Label.of("ID المالك الجديد", userInput)).build()).queue();
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
            case "voice_silence":
                channel.getManager().putRolePermissionOverride(event.getGuild().getPublicRole().getIdLong(), null, EnumSet.of(Permission.VOICE_SPEAK)).queue();
                event.reply("🔇 تم إسكات الجميع بنجاح.").setEphemeral(true).queue();
                break;
            case "voice_unsilence":
                channel.getManager().putRolePermissionOverride(event.getGuild().getPublicRole().getIdLong(), EnumSet.of(Permission.VOICE_SPEAK), null).queue();
                event.reply("🔊 تم إلغاء إسكات الجميع بنجاح.").setEphemeral(true).queue();
                break;
            case "voice_ghost":
                channel.getManager().putRolePermissionOverride(event.getGuild().getPublicRole().getIdLong(), null, EnumSet.of(Permission.VIEW_CHANNEL)).queue();
                event.reply("🎭 تم تفعيل وضع الشبح بنجاح.").setEphemeral(true).queue();
                break;
            case "voice_unghost":
                channel.getManager().putRolePermissionOverride(event.getGuild().getPublicRole().getIdLong(), EnumSet.of(Permission.VIEW_CHANNEL), null).queue();
                event.reply("🕶️ تم إلغاء وضع الشبح بنجاح.").setEphemeral(true).queue();
                break;
        }
    }

    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        String modalId = event.getModalId();
        if (!modalId.startsWith("modal_voice_")) return;
        
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
        } else if (modalId.equals("modal_voice_kick")) {
            String userId = event.getValue("voice_user_id").getAsString();
            event.getGuild().retrieveMemberById(userId).queue(member -> {
                if (channel.getMembers().contains(member)) {
                    event.getGuild().kickVoiceMember(member).queue();
                    event.reply("👞 تم طرد " + member.getAsMention() + " من الغرفة.").setEphemeral(true).queue();
                } else {
                    event.reply("❌ هذا العضو ليس متواجداً في غرفتك حالياً.").setEphemeral(true).queue();
                }
            }, err -> event.reply("❌ لم يتم العثور على عضو بهذا الأيدي.").setEphemeral(true).queue());
        } else if (modalId.equals("modal_voice_permit")) {
            String userId = event.getValue("voice_user_id").getAsString();
            event.getGuild().retrieveMemberById(userId).queue(member -> {
                channel.getManager().putMemberPermissionOverride(member.getIdLong(), EnumSet.of(Permission.VOICE_CONNECT), null).queue();
                event.reply("✅ تم السماح لـ " + member.getAsMention() + " بدخول الغرفة.").setEphemeral(true).queue();
            }, err -> event.reply("❌ لم يتم العثور على عضو بهذا الأيدي.").setEphemeral(true).queue());
        } else if (modalId.equals("modal_voice_reject")) {
            String userId = event.getValue("voice_user_id").getAsString();
            event.getGuild().retrieveMemberById(userId).queue(member -> {
                channel.getManager().putMemberPermissionOverride(member.getIdLong(), null, EnumSet.of(Permission.VOICE_CONNECT)).queue();
                if (channel.getMembers().contains(member)) event.getGuild().kickVoiceMember(member).queue();
                event.reply("❌ تم منع " + member.getAsMention() + " من دخول الغرفة.").setEphemeral(true).queue();
            }, err -> event.reply("❌ لم يتم العثور على عضو بهذا الأيدي.").setEphemeral(true).queue());
        } else if (modalId.equals("modal_voice_trust")) {
            String userId = event.getValue("voice_user_id").getAsString();
            event.getGuild().retrieveMemberById(userId).queue(member -> {
                channel.getManager().putMemberPermissionOverride(member.getIdLong(), EnumSet.of(Permission.VIEW_CHANNEL, Permission.VOICE_CONNECT), null).queue();
                event.reply("🤝 تم توثيق " + member.getAsMention() + " في الغرفة.").setEphemeral(true).queue();
            }, err -> event.reply("❌ لم يتم العثور على عضو بهذا الأيدي.").setEphemeral(true).queue());
        } else if (modalId.equals("modal_voice_untrust")) {
            String userId = event.getValue("voice_user_id").getAsString();
            event.getGuild().retrieveMemberById(userId).queue(member -> {
                channel.getManager().putMemberPermissionOverride(member.getIdLong(), null, EnumSet.of(Permission.VIEW_CHANNEL)).queue();
                event.reply("🚫 تم إلغاء توثيق " + member.getAsMention() + ".").setEphemeral(true).queue();
            }, err -> event.reply("❌ لم يتم العثور على عضو بهذا الأيدي.").setEphemeral(true).queue());
        } else if (modalId.equals("modal_voice_transfer")) {
            String userId = event.getValue("voice_user_id").getAsString();
            event.getGuild().retrieveMemberById(userId).queue(member -> {
                voiceRoomRepository.findByChannelId(channel.getId()).ifPresent(room -> {
                    room.setOwnerId(member.getId());
                    voiceRoomRepository.save(room);
                    channel.getManager().putMemberPermissionOverride(member.getIdLong(), EnumSet.of(Permission.VIEW_CHANNEL, Permission.VOICE_MOVE_OTHERS, Permission.MANAGE_CHANNEL), null).queue();
                    event.reply("👑 تم نقل ملكية الغرفة بنجاح إلى " + member.getAsMention() + ".").setEphemeral(true).queue();
                });
            }, err -> event.reply("❌ لم يتم العثور على عضو بهذا الأيدي.").setEphemeral(true).queue());
        }
    }
}
