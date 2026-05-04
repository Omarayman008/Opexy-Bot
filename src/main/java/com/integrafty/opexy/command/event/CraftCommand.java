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
        return List.of(Commands.slash("craft", "بدء لعبة التخمين من خلال الصناعة (فردية)")
                .addOptions(new OptionData(OptionType.INTEGER, "reward", "قيمة الجائزة بالـ opex", true)));
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("craft")) return;

        int reward = event.getOption("reward").getAsInt();
        long userId = event.getUser().getIdLong();
        
        String grid = craftManager.startCraft(userId, reward, event.getGuild(), event.getMember());

        String description = String.format("أمامك طاولة كرافتنق خاصة بك يا %s... خمن ما هو الشيء الذي يتم صنعه؟\n\n", event.getUser().getAsMention()) +
                grid + "\n" +
                "💰 الجائزة: **" + reward + " opex**\n\n" +
                "💡 اكتب الإجابة مباشرة في الشات!";

        event.replyEmbeds(EmbedUtil.containerBranded("CRAFTING", "🛠️ ماذا نصنع؟", description, EmbedUtil.BANNER_MAIN).getEmbeds().get(0)).queue();
    }
}
