package com.integrafty.opexy.command.event;

import com.integrafty.opexy.command.base.MultiSlashCommand;
import com.integrafty.opexy.service.event.AchievementService;
import com.integrafty.opexy.service.event.EventManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Component
public class SpeedChallengeCommand implements MultiSlashCommand {

    private final EventManager eventManager;
    private final AchievementService achievementService;
    private final Random random = new Random();

    @Value("${opexy.roles.hype-manager}")
    private String hypeManagerId;

    @Value("${opexy.roles.hype-events}")
    private String hypeEventsId;

    private static final List<String> MINECRAFT_WORDS = List.of(
            "دايموند", "نذررايت", "كريبر", "أندر مان", "بلوكة طين", "سيف حديدي", 
            "درع ذهبي", "خشب محلل", "بوابة النذر", "تنين الاندر", "قرية القرويين",
            "صندوق مخفي", "بيوم الغابة", "كهف عميق", "منجم قديم", "خيوط عنكبوت"
    );

    public SpeedChallengeCommand(EventManager eventManager, AchievementService achievementService) {
        this.eventManager = eventManager;
        this.achievementService = achievementService;
    }

    @Override
    public List<SlashCommandData> getCommandDataList() {
        return List.of(Commands.slash("speed", "بدء تحدي الـ 7 ثواني (Minecraft Edition)")
                .addOptions(new OptionData(OptionType.STRING, "difficulty", "الصعوبة", true)
                        .addChoice("سهل (Easy)", "easy")
                        .addChoice("متوسط (Medium)", "medium")
                        .addChoice("صعب (Hard)", "hard")));
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("speed")) return;

        String difficulty = event.getOption("difficulty") != null ? event.getOption("difficulty").getAsString() : "easy";
        int reward = difficulty.equals("easy") ? 35 : difficulty.equals("medium") ? 55 : 70;

        String word = MINECRAFT_WORDS.get(random.nextInt(MINECRAFT_WORDS.size()));
        String body = "أسرع شخص يكتب الكلمة التالية يربح **" + reward + " opex**!\n\nالكلمة هي:\n**" + word + "**";

        event.reply(new net.dv8tion.jda.api.utils.messages.MessageCreateBuilder()
                .setComponents(com.integrafty.opexy.utils.EmbedUtil.containerBranded("SPEED", "⚡ تحدي الـ 7 ثواني!", body, com.integrafty.opexy.utils.EmbedUtil.BANNER_MAIN))
                .useComponentsV2(true).build())
                .useComponentsV2(true).queue(hook -> {
            event.getChannel().getIterableHistory().takeAsync(1).thenAccept(messages -> {
                long startTime = System.currentTimeMillis();
                
                // Wait for responses for 7 seconds
                event.getJDA().addEventListener(new net.dv8tion.jda.api.hooks.ListenerAdapter() {
                    private boolean finished = false;

                    @Override
                    public void onMessageReceived(net.dv8tion.jda.api.events.message.MessageReceivedEvent msgEvent) {
                        if (finished || msgEvent.getAuthor().isBot() || !msgEvent.getChannel().equals(event.getChannel())) return;
                        
                        String content = msgEvent.getMessage().getContentRaw().trim();
                        if (content.equalsIgnoreCase(word) || content.replace("أ", "ا").replace("ة", "ه").equalsIgnoreCase(word.replace("أ", "ا").replace("ة", "ه"))) {
                            long timeTaken = System.currentTimeMillis() - startTime;
                            if (timeTaken <= 7000) {
                                finished = true;
                                msgEvent.getMessage().reply("✅ مبروك! لقد فزت بـ " + reward + " opex في " + (timeTaken / 1000.0) + " ثانية!").queue();
                                achievementService.updateStats(msgEvent.getAuthor().getIdLong(), event.getGuild(), stats -> {
                                    stats.setSpeedWins(stats.getSpeedWins() + 1);
                                });
                                event.getJDA().removeEventListener(this);
                            }
                        }
                    }
                });

                // Timeout task
                event.getChannel().sendMessage("⏳ بدأ العد التنازلي...").queueAfter(7, TimeUnit.SECONDS, msg -> {
                    // Cleanup logic if needed
                });
            });
        });
    }
}
