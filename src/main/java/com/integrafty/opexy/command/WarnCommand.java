package com.integrafty.opexy.command;

import com.integrafty.opexy.entity.UserEntity;
import com.integrafty.opexy.repository.UserRepository;
import com.integrafty.opexy.command.base.SlashCommand;
import com.integrafty.opexy.utils.EmbedUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.components.container.Container;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WarnCommand implements SlashCommand {

    private final UserRepository userRepository;

    private static final String ROLE_WARN_1 = "1487196789399490711";
    private static final String ROLE_WARN_2 = "1487196790892794067";
    private static final String ROLE_WARN_3 = "1487196791144190143";

    @Override
    public String getName() {
        return "warn";
    }

    @Override
    public SlashCommandData getCommandData() {
        return Commands.slash("warn", "تحذير عضو")
                .addOption(OptionType.USER, "user", "العضو المراد تحذيره", true)
                .addOption(OptionType.STRING, "reason", "سبب التحذير", false);
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        if (!event.getMember().hasPermission(Permission.KICK_MEMBERS)) {
            Container denied = EmbedUtil.accessDenied();
            MessageCreateBuilder deniedBuilder = new MessageCreateBuilder();
            deniedBuilder.setComponents(denied);
            deniedBuilder.useComponentsV2(true);
            event.reply(deniedBuilder.build()).setEphemeral(true).useComponentsV2(true).queue();
            return;
        }

        Member target = event.getOption("user").getAsMember();
        String reason = event.getOption("reason") != null ? event.getOption("reason").getAsString() : "بدون سبب";

        if (target == null) {
            Container error = EmbedUtil.error("ERROR", "لم يتم العثور على العضو.");
            MessageCreateBuilder errorBuilder = new MessageCreateBuilder();
            errorBuilder.setComponents(error);
            errorBuilder.useComponentsV2(true);
            event.reply(errorBuilder.build()).setEphemeral(true).useComponentsV2(true).queue();
            return;
        }

        // Update Warning Count in Database
        String guildId = event.getGuild().getId();
        UserEntity user = userRepository.findByUserIdAndGuildId(target.getId(), guildId)
                .orElse(new UserEntity(target.getId(), guildId, 0, 0, false, null, null, 0));
        
        user.setWarningCount(user.getWarningCount() + 1);
        userRepository.save(user);

        int count = user.getWarningCount();

        // Assign Role based on count
        String roleIdToAdd = switch (count) {
            case 1 -> ROLE_WARN_1;
            case 2 -> ROLE_WARN_2;
            case 3 -> ROLE_WARN_3;
            default -> count > 3 ? ROLE_WARN_3 : null;
        };

        if (roleIdToAdd != null) {
            Role role = event.getGuild().getRoleById(roleIdToAdd);
            if (role != null) {
                event.getGuild().addRoleToMember(target, role).queue();
            }
        }

        String description = String.format("### ⚠️ Warn Notification\n\n**Target:** %s\n**Moderator:** %s\n**Warning Count:** %d\n**Reason:** %s", 
                target.getAsMention(), event.getMember().getAsMention(), count, reason);

        Container container = EmbedUtil.containerBranded("MODERATION", "User Warned", description, EmbedUtil.BANNER_MAIN);
        
        MessageCreateBuilder builder = new MessageCreateBuilder();
        builder.setComponents(container);
        builder.useComponentsV2(true);

        event.reply(builder.build()).useComponentsV2(true).queue();
        
        target.getUser().openPrivateChannel().queue(pc -> {
            pc.sendMessage("تم تحذيرك في سيرفر " + event.getGuild().getName() + " (تحذير رقم " + count + ") بسبب: " + reason).queue(null, e -> {});
        });
    }
}
