package com.integrafty.opexy.command.event;

import com.integrafty.opexy.command.base.MultiSlashCommand;
import com.integrafty.opexy.service.event.AchievementService;
import com.integrafty.opexy.service.event.EventManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class ScavengerHuntCommand implements MultiSlashCommand {

    private final EventManager eventManager;
    private final AchievementService achievementService;
    private final Random random = new Random();

    @Value("${opexy.roles.hype-manager}")
    private String hypeManagerId;

    @Value("${opexy.roles.hype-events}")
    private String hypeEventsId;

    public ScavengerHuntCommand(EventManager eventManager, AchievementService achievementService) {
        this.eventManager = eventManager;
        this.achievementService = achievementService;
    }

    @Override
    public List<SlashCommandData> getCommandDataList() {
        return List.of(Commands.slash("hunt", "بدء فعالية الصيد (Scavenger Hunt)"));
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("hunt")) return;

        // Check permissions
        boolean hasRole = event.getMember().getRoles().stream()
                .anyMatch(r -> r.getId().equals(hypeManagerId) || r.getId().equals(hypeEventsId));
        
        if (!hasRole) {
            event.reply("❌ عذراً، هذا الأمر مخصص لمشرفي الفعاليات فقط.").setEphemeral(true).queue();
            return;
        }

        List<TextChannel> channels = event.getGuild().getTextChannels().stream()
                .filter(ch -> ch.canTalk())
                .collect(Collectors.toList());

        if (channels.isEmpty()) {
            event.reply("❌ لم يتم العثور على قنوات متاحة لإخفاء الكود.").setEphemeral(true).queue();
            return;
        }

        TextChannel targetChannel = channels.get(random.nextInt(channels.size()));
        String code = "OPEXY-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("🔎 فعالية الصيد بدأت!")
                .setColor(Color.ORANGE)
                .setDescription("تم إخفاء كود سري في إحدى قنوات السيرفر!\n\n**المهمة:** ابحث عن الكود واكتبه هنا في الشات لتفوز بجوائز opex!")
                .addField("الصعوبة", "عشوائي", true)
                .setFooter("أول شخص يكتب الكود يفوز!");

        event.replyEmbeds(embed.build()).queue();
        
        // Notify the supervisor (Ephemeral)
        event.getHook().sendMessage("🤫 الكود السري هو: **" + code + "**\nتم إخفاؤه في قناة: " + targetChannel.getAsMention())
                .setEphemeral(true).queue();
    }
}
