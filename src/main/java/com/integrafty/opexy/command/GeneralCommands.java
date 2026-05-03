package com.integrafty.opexy.command;

import com.integrafty.opexy.command.base.MultiSlashCommand;
import com.integrafty.opexy.utils.EmbedUtil;
import com.integrafty.opexy.service.TranslationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.emoji.CustomEmoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.components.container.Container;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class GeneralCommands implements MultiSlashCommand {

    private final TranslationService translationService;

    @Override
    public List<SlashCommandData> getCommandDataList() {
        List<SlashCommandData> list = new ArrayList<>();

        list.add(Commands.slash("help", "قـــائـــمـــة الـــمـــســـاعـــدة الـــخـــاصـــة بـــالـــبـــوت"));
        list.add(Commands.slash("ping", "عـــرض ســـرعـــة اتـــصـــال الـــبـــوت الـــحـــالـــيـــة"));
        list.add(Commands.slash("roll", "رمـــي حـــجـــر الـــنـــرد الـــعـــشـــوائـــي"));
        
        list.add(Commands.slash("colors", "عـــرض رتـــب الألـــوان الـــمـــتـــاحـــة فـــي الـــســـيـــرفـــر"));
        list.add(Commands.slash("color-set", "تـــحـــديـــد لـــون رتـــبـــتـــك الـــخـــاصـــة")
                .addOption(OptionType.ROLE, "role", "اخـــتـــر رتـــبـــة الـــلـــون", true));

        list.add(Commands.slash("translate", "تـــرجـــمـــة الـــنـــص إلـــى لـــغـــة مـــعـــيـــنـــة")
                .addOption(OptionType.STRING, "text", "الـــنـــص", true)
                .addOption(OptionType.STRING, "language", "الـــلـــغـــة", true));

        list.add(Commands.slash("get-emojis", "الـــحـــصـــول عـــلـــى تـــفـــاصـــيـــل الإيـــمـــوجـــي الـــمـــســـتـــخـــدم")
                .addOption(OptionType.STRING, "emoji", "الإيـــمـــوجـــي", true));

        return list;
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        String name = event.getName().toLowerCase();

        switch (name) {
            case "help" -> handleHelp(event);
            case "ping" -> handlePing(event);
            case "roll" -> handleRoll(event);
            case "colors" -> handleColors(event);
            case "color-set" -> handleColorSet(event);
            case "translate" -> handleTranslate(event);
            case "get-emojis" -> handleGetEmojis(event);
        }
    }

    private void handleHelp(SlashCommandInteractionEvent event) {
        String body = """
                ### 🛡️ Moderation
                `/ban`, `/kick`, `/mute-text`, `/timeout`, `/clear`, `/role`, `/warn-add`, `/lock`, `/hide`
                
                ### 📡 Info
                `/user`, `/server`, `/avatar`, `/roles`, `/banner`, `/profile`
                
                ### ⚙️ General
                `/ping`, `/roll`, `/colors`, `/translate`, `/get-emojis`
                
                *Use `/` to see all available commands and their descriptions.*
                """;
        reply(event, EmbedUtil.containerBranded("Opexy Hub", "System Directory", body, EmbedUtil.BANNER_MAIN));
    }

    private void handlePing(SlashCommandInteractionEvent event) {
        long gatewayPing = event.getJDA().getGatewayPing();
        reply(event, EmbedUtil.containerBranded("SYSTEM", "Latency Protocol", "### 📡 Status\nGateway Ping: `" + gatewayPing + "ms`", EmbedUtil.BANNER_MAIN));
    }

    private void handleRoll(SlashCommandInteractionEvent event) {
        int res = new Random().nextInt(6) + 1;
        reply(event, EmbedUtil.containerBranded("GAME", "Dice Roll", "### 🎲 Result\nYou rolled a **" + res + "**", EmbedUtil.BANNER_MAIN));
    }

    private static final Map<String, String> COLOR_ROLES = Map.of(
        "color_red", "1499885720209195059",
        "color_turquoise", "1499885336703275029",
        "color_orange", "1499885645563166914",
        "color_gray", "1499885533277589656",
        "color_navy", "1499885778413813810",
        "color_blurple", "1499884810338832394",
        "color_asphalt", "1499885394752176190"
    );

    private void handleColors(SlashCommandInteractionEvent event) {
        String body = """
                ### 🎨 نـــظـــام الألـــوان | COLOR SYSTEM
                ▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬
                
                اخـــتـــر لـــونـــك الـــمـــفـــضـــل مـــن الأزرار أدـنـــاه.
                *ســـيـــتـــم تـــحـــديـــث لـــون رتـــبـــتـــك تـــلـــقـــائـــيـــاً.*
                
                ▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬
                """;

        net.dv8tion.jda.api.components.buttons.Button b1 = net.dv8tion.jda.api.components.buttons.Button.secondary("color_red", "Soft Red");
        net.dv8tion.jda.api.components.buttons.Button b2 = net.dv8tion.jda.api.components.buttons.Button.secondary("color_turquoise", "Turquoise");
        net.dv8tion.jda.api.components.buttons.Button b3 = net.dv8tion.jda.api.components.buttons.Button.secondary("color_orange", "Carrot Orange");
        net.dv8tion.jda.api.components.buttons.Button b4 = net.dv8tion.jda.api.components.buttons.Button.secondary("color_gray", "Light Gray");
        net.dv8tion.jda.api.components.buttons.Button b5 = net.dv8tion.jda.api.components.buttons.Button.secondary("color_navy", "Midnight Navy");
        
        net.dv8tion.jda.api.components.buttons.Button b6 = net.dv8tion.jda.api.components.buttons.Button.secondary("color_blurple", "Blurple");
        net.dv8tion.jda.api.components.buttons.Button b7 = net.dv8tion.jda.api.components.buttons.Button.secondary("color_asphalt", "Wet Asphalt");

        net.dv8tion.jda.api.components.actionrow.ActionRow row1 = net.dv8tion.jda.api.components.actionrow.ActionRow.of(b1, b2, b3, b4, b5);
        net.dv8tion.jda.api.components.actionrow.ActionRow row2 = net.dv8tion.jda.api.components.actionrow.ActionRow.of(b6, b7);

        Container container = EmbedUtil.containerBranded("IDENTITY", "Color Selection", body, EmbedUtil.BANNER_MAIN, row1, row2);
        reply(event, container);
    }

    private void handleColorSet(SlashCommandInteractionEvent event) {
        Role targetRole = event.getOption("role").getAsRole();

        if (targetRole == null) {
            replyEphemeral(event, EmbedUtil.error("INVALID SELECTION", "الـــرتـــبـــة الـــمـــخـــتـــارة غـــيـــر صـــحـــيـــحـــة."));
            return;
        }

        // Optional: Check if the role is a "Color" role (e.g. has no permissions or specific name)
        // For now, we trust the user's selection but we remove other roles that are numeric-named
        // as per the previous logic's pattern.

        // Remove old color roles (assuming roles named with numbers are color roles)
        List<Role> oldColors = event.getMember().getRoles().stream()
                .filter(r -> r.getName().matches("\\d+"))
                .collect(Collectors.toList());
        
        for (Role r : oldColors) {
            if (!r.equals(targetRole)) {
                event.getGuild().removeRoleFromMember(event.getMember(), r).queue();
            }
        }

        event.getGuild().addRoleToMember(event.getMember(), targetRole).queue(v -> {
            reply(event, EmbedUtil.success("Identity Update", "تـــم تـــحـــديـــد رتـــبـــة الـــلـــون [" + targetRole.getName() + "] بـــنـــجـــاح."));
        }, error -> {
            replyEphemeral(event, EmbedUtil.error("PERMISSION ERROR", "لا أمـــلـــك صـــلاحـــيـــات لـــإعـــطـــائـــك هـــذه الـــرتـــبـــة."));
        });
    }

    private void handleTranslate(SlashCommandInteractionEvent event) {
        String text = event.getOption("text").getAsString();
        String lang = event.getOption("language").getAsString();
        
        event.deferReply().queue();

        String result = translationService.translate(text, lang);
        
        String body = String.format("""
                ### 🌐 نـــتـــيـــجـــة الـــتـــرجـــمـــة | TRANSLATION RESULT
                ▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬
                
                ▫️ **الـــلـــغـــة الـــمـــســـتـــهـــدفـــة:** `%s`
                ▫️ **الـــنـــص الـــأصـــلـــي:**
                > %s
                
                ▫️ **الـــتـــرجـــمـــة:**
                > %s
                
                ▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬
                """, lang.toUpperCase(), text, result);

        Container container = EmbedUtil.containerBranded("LANGUAGE", "Translation Engine", body, EmbedUtil.BANNER_MAIN);
        event.getHook().sendMessage(new MessageCreateBuilder().setComponents(container).useComponentsV2(true).build()).useComponentsV2(true).queue();
    }

    private void handleGetEmojis(SlashCommandInteractionEvent event) {
        String emojiStr = event.getOption("emoji").getAsString();
        
        String body;
        String imageUrl = null;

        try {
            Emoji emoji = Emoji.fromFormatted(emojiStr);
            if (emoji instanceof CustomEmoji custom) {
                body = String.format("""
                        ### 🔍 تـــفـــاصـــيـــل الإيـــمـــوجـــي | EMOJI DETAILS
                        ▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬
                        
                        ▫️ **الاســـم:** `%s`
                        ▫️ **الـــمـــعـــرف (ID):** `%s`
                        ▫️ **الـــصـــيـــغـــة:** `<:%s:%s>`
                        ▫️ **رابط الصورة:** [Click Here](%s)
                        
                        ▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬
                        """, custom.getName(), custom.getId(), custom.getName(), custom.getId(), custom.getImageUrl());
                imageUrl = custom.getImageUrl();
            } else {
                body = String.format("""
                        ### 🔍 تـــفـــاصـــيـــل الإيـــمـــوجـــي | EMOJI DETAILS
                        ▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬
                        
                        ▫️ **الـــنـــوع:** إيـــمـــوجـــي افـــتـــراضـــي (Standard)
                        ▫️ **الـــرمـــز:** %s
                        ▫️ **الاســـم:** `%s`
                        
                        ▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬
                        """, emoji.getName(), emoji.getName());
            }
        } catch (Exception e) {
            body = "❌ لـــم يـــتـــم الـــتـــعـــرف عـــلـــى الإيـــمـــوجـــي.";
        }

        reply(event, EmbedUtil.containerBranded("ASSET", "Emoji Inspection", body, imageUrl != null ? imageUrl : EmbedUtil.BANNER_MAIN));
    }

    private void reply(SlashCommandInteractionEvent e, Container c) {
        var msg = new MessageCreateBuilder().setComponents(c).useComponentsV2(true).build();
        e.reply(msg).useComponentsV2(true).queue();
    }

    private void replyEphemeral(SlashCommandInteractionEvent e, Container c) {
        var msg = new MessageCreateBuilder().setComponents(c).useComponentsV2(true).build();
        e.reply(msg).setEphemeral(true).useComponentsV2(true).queue();
    }
}
