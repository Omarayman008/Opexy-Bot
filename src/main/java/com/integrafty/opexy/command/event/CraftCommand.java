package com.integrafty.opexy.command.event;

import com.integrafty.opexy.command.base.MultiSlashCommand;
import com.integrafty.opexy.service.event.CraftManager;
import com.integrafty.opexy.service.event.EventManager;
import com.integrafty.opexy.utils.EmbedUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CraftCommand implements MultiSlashCommand {

    private final EventManager eventManager;
    private final CraftManager craftManager;

    public CraftCommand(EventManager eventManager, CraftManager craftManager) {
        this.eventManager = eventManager;
        this.craftManager = craftManager;
    }

    @Override
    public List<SlashCommandData> getCommandDataList() {
        return List.of(Commands.slash("craft", "بدء فعالية التخمين من خلال الصناعة (Staff Only)")
                .addOptions(new OptionData(OptionType.INTEGER, "reward", "قيمة الجائزة بالـ opex", true))
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR)));
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("craft")) return;

        if (eventManager.isEventActive()) {
            event.replyEmbeds(EmbedUtil.error("EVENT ACTIVE", "هناك فعالية جارية حالياً، يرجى انتظار انتهائها.").getEmbeds().get(0))
                    .setEphemeral(true).queue();
            return;
        }

        int reward = event.getOption("reward").getAsInt();
        eventManager.startGroupEvent("تخمين الصناعة");
        
        String grid = craftManager.startCraft(reward, event.getGuild(), event.getMember());

        String description = "أمامك طاولة كرافتنق تحتوي على أغراض... خمن ما هو الشيء الذي يتم صنعه؟\n\n" +
                grid + "\n" +
                "💰 الجائزة: **%d opex**\n\n" +
                "💡 اكتب الإجابة مباشرة في الشات!";

        event.replyEmbeds(EmbedUtil.containerBranded("CRAFTING", "🛠️ ماذا نصنع؟", String.format(description, reward), EmbedUtil.BANNER_MAIN).getEmbeds().get(0)).queue();
    }
}
