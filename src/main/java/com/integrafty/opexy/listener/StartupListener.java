package com.integrafty.opexy.listener;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class StartupListener extends ListenerAdapter {

    private final JDA jda;

    private static final Map<String, String> COLOR_ROLES = Map.of(
        "color_red", "1499885720209195059",
        "color_turquoise", "1499885336703275029",
        "color_orange", "1499885645563166914",
        "color_gray", "1499885533277589656",
        "color_navy", "1499885778413813810",
        "color_blurple", "1499884810338832394",
        "color_asphalt", "1499885394752176190"
    );

    private static final Map<String, String> PING_ROLES = Map.of(
        "ping_stream", "1487196786488770610",
        "ping_minecraft", "1487196787142819961",
        "ping_event", "1487196787893731428",
        "ping_mcserver", "1499896841150402692",
        "ping_dcserver", "1499896994003681310",
        "ping_apply", "1499897044897366056"
    );

    @PostConstruct
    public void init() {
        jda.addEventListener(this);
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String id = event.getComponentId();

        if (id.startsWith("color_")) {
            handleColorSelection(event, id);
        } else if (id.startsWith("ping_")) {
            handlePingSelection(event, id);
        } else if (id.equals("support_direct")) {
            event.reply("📍 توجه إلى روم الدعم الفني من هنا: <#1487143271586074624>").setEphemeral(true).queue();
        }
    }

    private void handleColorSelection(ButtonInteractionEvent event, String buttonId) {
        String targetRoleId = COLOR_ROLES.get(buttonId);
        if (targetRoleId == null) return;

        Role targetRole = event.getGuild().getRoleById(targetRoleId);
        if (targetRole == null) return;

        event.deferReply(true).queue();

        // Remove other color roles first (Exclusive)
        for (String roleId : COLOR_ROLES.values()) {
            Role r = event.getGuild().getRoleById(roleId);
            if (r != null && event.getMember().getRoles().contains(r)) {
                event.getGuild().removeRoleFromMember(event.getMember(), r).queue();
            }
        }

        // Add the new role
        event.getGuild().addRoleToMember(event.getMember(), targetRole).queue(
            success -> event.getHook().sendMessage("✅ تـم تـحـديـث لـونـك إلـى: **" + targetRole.getName() + "**").queue()
        );
    }

    private void handlePingSelection(ButtonInteractionEvent event, String buttonId) {
        String targetRoleId = PING_ROLES.get(buttonId);
        if (targetRoleId == null) return;

        Role targetRole = event.getGuild().getRoleById(targetRoleId);
        if (targetRole == null) return;

        event.deferReply(true).queue();

        if (event.getMember().getRoles().contains(targetRole)) {
            event.getGuild().removeRoleFromMember(event.getMember(), targetRole).queue(
                success -> event.getHook().sendMessage("🔕 تـم إزالـة تـنـبـيـهات: **" + targetRole.getName() + "**").queue()
            );
        } else {
            event.getGuild().addRoleToMember(event.getMember(), targetRole).queue(
                success -> event.getHook().sendMessage("🔔 تـم تـفـعـيـل تـنـبـيـهات: **" + targetRole.getName() + "**").queue()
            );
        }
    }
}
