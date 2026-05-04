package com.integrafty.opexy.service.event;

import com.integrafty.opexy.utils.EmbedUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.interactions.components.textinput.TextInput;
import net.dv8tion.jda.api.interactions.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.*;

@Service
@RequiredArgsConstructor
public class JawlahManager extends ListenerAdapter {

    private final JDA jda;
    private final EventManager eventManager;
    private final Map<Long, JawlahGame> activeGames = new HashMap<>();

    @PostConstruct
    public void init() {
        jda.addEventListener(this);
    }

    public void initiateSetup(net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent event) {
        Modal modal = Modal.create("jawlah_setup_modal", "إعـــدادات لـــعـــبـــة جـــولـــة")
                .addComponents(
                        ActionRow.of(TextInput.create("game_name", "اســـم الـــلـــعـــبـــة", TextInputStyle.SHORT).setRequired(true).setPlaceholder("مـــثـــال: تـــحـــدي الـــعـــمـــالـــقـــة").build()),
                        ActionRow.of(TextInput.create("team_a_name", "اســـم الـــفـــريـــق الأول", TextInputStyle.SHORT).setRequired(true).setPlaceholder("مـــثـــال: فـــريـــق الـــصـــقـــور").build()),
                        ActionRow.of(TextInput.create("team_b_name", "اســـم الـــفـــريـــق الـــثـــانـــي", TextInputStyle.SHORT).setRequired(true).setPlaceholder("مـــثـــال: فـــريـــق الـــنـــســـور").build())
                ).build();
        event.replyModal(modal).queue();
    }

    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        if (!event.getModalId().equals("jawlah_setup_modal")) return;

        String gameName = event.getValue("game_name").getAsString();
        String teamA = event.getValue("team_a_name").getAsString();
        String teamB = event.getValue("team_b_name").getAsString();

        JawlahGame game = new JawlahGame(event.getChannel().getIdLong(), event.getUser().getIdLong());
        game.setGameName(gameName);
        game.setTeamAName(teamA);
        game.setTeamBName(teamB);
        activeGames.put(event.getChannel().getIdLong(), game);

        // Send Helping Hands Selection
        sendHelpingHandsSelection(event);
    }

    private void sendHelpingHandsSelection(ModalInteractionEvent event) {
        String body = "### 🛠️ وســـائـــل الـــمـــســـاعـــدة\nاخـــتـــر وســـائـــل الـــمـــســـاعـــدة الـــمـــتـــاحـــة لـــكـــل فـــريـــق فـــي هـــذه الـــجـــولـــة.";
        
        event.reply(new MessageCreateBuilder()
                .setComponents(EmbedUtil.containerBranded("SETUP", "حـــدّد وســـائـــل الـــمـــســـاعـــدة", body, EmbedUtil.BANNER_MAIN,
                        ActionRow.of(
                                Button.secondary("jawlah_help_1", "جاوب جوابين ✌️"),
                                Button.secondary("jawlah_help_2", "اتصال بصديق 📞"),
                                Button.secondary("jawlah_help_3", "الحفرة ⛳"),
                                Button.secondary("jawlah_help_4", "اعكس الدور 🔄"),
                                Button.secondary("jawlah_help_5", "السؤال الذهبي 🏆")
                        ),
                        ActionRow.of(Button.success("jawlah_start_confirm", "بـــدء الـــلـــعـــب 🚀"))
                ))
                .useComponentsV2(true).build()).queue();
    }

    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {
        String id = event.getComponentId();
        if (!id.startsWith("jawlah_")) return;

        JawlahGame game = activeGames.get(event.getChannel().getIdLong());
        if (game == null) return;

        if (event.getUser().getIdLong() != game.organizerId) {
            event.reply("❌ عذراً، المنظم فقط يمكنه التحكم في اللعبة.").setEphemeral(true).queue();
            return;
        }

        if (id.equals("jawlah_category")) {
            game.setSelectedCategory(event.getValues().get(0));
            event.reply("تم اختيار الفئة: **" + game.getSelectedCategory() + "**. الآن اختر القيمة.").setEphemeral(true).queue();
        } else if (id.equals("jawlah_value")) {
            game.setSelectedValue(Integer.parseInt(event.getValues().get(0)));
            showQuestionPrompt(event, game);
        }
    }

    private void showQuestionPrompt(StringSelectInteractionEvent event, JawlahGame game) {
        String body = String.format("### ❓ ســـؤال الـــتـــحـــدي\n\n" +
                "**الـفـئـة:** `%s`\n" +
                "**الـقـيـمـة:** `%d` نـقـطـة\n" +
                "**الـدور لـفـريـق:** %s\n\n" +
                "انـتـظـر الإجـابـة مـن الـفـريـق ثـم اضـغـط عـلـى الـتـقـيـيـم الـمـنـاسـب.",
                game.selectedCategory, game.selectedValue, game.turnA ? game.teamAName : game.teamBName);

        event.editMessage(new MessageEditBuilder()
                .setComponents(EmbedUtil.containerBranded("QUESTION", "🔍 جـــاري بـــحـــث الـــســـؤال...", body, EmbedUtil.BANNER_MAIN,
                        ActionRow.of(
                                Button.success("jawlah_correct", "إجـابـة صـحـيـحـة ✅"),
                                Button.danger("jawlah_wrong", "إجـابـة خـاطـئـة ❌"),
                                Button.secondary("jawlah_back", "الـعـودة لـلـوحـة ⬅️")
                        )
                ))
                .useComponentsV2(true).build()).queue();
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String id = event.getComponentId();
        if (!id.startsWith("jawlah_")) return;

        JawlahGame game = activeGames.get(event.getChannel().getIdLong());
        if (game == null) return;

        if (event.getUser().getIdLong() != game.organizerId) {
            event.reply("❌ عذراً، المنظم فقط يمكنه التحكم في اللعبة.").setEphemeral(true).queue();
            return;
        }

        switch (id) {
            case "jawlah_correct" -> {
                int points = game.selectedValue;
                if (game.isGoldenQuestion()) points *= 2;
                
                if (game.turnA) {
                    game.scoreA += points;
                    if (game.isPitActive()) game.scoreB = Math.max(0, game.scoreB - points);
                } else {
                    game.scoreB += points;
                    if (game.isPitActive()) game.scoreA = Math.max(0, game.scoreA - points);
                }
                
                // Reset modifiers
                game.setGoldenQuestion(false);
                game.setPitActive(false);
                game.setTurnA(!game.turnA); 
                sendBoard(event);
            }
            case "jawlah_wrong" -> {
                game.setGoldenQuestion(false);
                game.setPitActive(false);
                game.setTurnA(!game.turnA); 
                sendBoard(event);
            }
            case "jawlah_back" -> sendBoard(event);
            case "jawlah_start_confirm" -> sendBoard(event);
            case "jawlah_stop" -> {
                activeGames.remove(event.getChannel().getIdLong());
                eventManager.endGroupEvent();
                event.reply("🛑 تم إنهاء لعبة جولة.").queue();
            }
            case "jawlah_help_1" -> event.reply("✅ تم تفعيل: **جاوب جوابين**").setEphemeral(true).queue();
            case "jawlah_help_2" -> event.reply("✅ تم تفعيل: **اتصال بصديق**").setEphemeral(true).queue();
            case "jawlah_help_3" -> {
                game.setPitActive(true);
                event.reply("⛳ تم تفعيل: **الحفرة** (سيتم خصم النقاط من الخصم عند الإجابة الصحيحة)").setEphemeral(true).queue();
            }
            case "jawlah_help_4" -> {
                game.setTurnA(!game.turnA);
                event.reply("🔄 تم تفعيل: **اعكس الدور**").setEphemeral(true).queue();
                sendBoard(event);
            }
            case "jawlah_help_5" -> {
                game.setGoldenQuestion(true);
                event.reply("🏆 تم تفعيل: **السؤال الذهبي** (النقاط مضاعفة!)").setEphemeral(true).queue();
            }
        }
    }

    private void sendBoard(ButtonInteractionEvent event) {
        JawlahGame game = activeGames.get(event.getChannel().getIdLong());
        
        String modifiers = (game.isPitActive() ? "⛳ **الـحـفـرة مـفـعـلـة**\n" : "") +
                           (game.isGoldenQuestion() ? "🏆 **الـسـؤال الـذهـبـي مـفـعـل**\n" : "");

        String body = String.format("### 🏆 %s\n\n" +
                "**الـنـتـيـجـة:**\n" +
                "🔵 **%s**: `%d` نـقـطـة\n" +
                "🔴 **%s**: `%d` نـقـطـة\n\n" +
                "%s" +
                "**الـدور الـحـالـي:** %s\n\n" +
                "يـرجـى اخـتـيـار الـفـئـة والـقـيـمـة مـن الـقـوائـم أدناه.",
                game.gameName, game.teamAName, game.scoreA, game.teamBName, game.scoreB, 
                modifiers, game.turnA ? "🔵 " + game.teamAName : "🔴 " + game.teamBName);

        StringSelectMenu categoryMenu = StringSelectMenu.create("jawlah_category")
                .setPlaceholder("اخـتـر الـفـئـة...")
                .addOption("كرة قدم ⚽", "football")
                .addOption("خمن 🔍", "guess")
                .addOption("حول العالم 🌍", "world")
                .addOption("عام 📚", "general")
                .addOption("إسلاميات 🌙", "islamic")
                .addOption("فن 🎨", "art")
                .build();

        StringSelectMenu valueMenu = StringSelectMenu.create("jawlah_value")
                .setPlaceholder("اخـتـر قـيـمـة الـنـقـاط...")
                .addOption("100 نقطة", "100")
                .addOption("200 نقطة", "200")
                .addOption("300 نقطة", "300")
                .addOption("400 نقطة", "400")
                .addOption("500 نقطة", "500")
                .addOption("600 نقطة", "600")
                .build();

        event.editMessage(new MessageEditBuilder()
                .setComponents(EmbedUtil.containerBranded("GAME", "🎮 لوحـة الـتـحـدي — Jawlah Board", body, EmbedUtil.BANNER_MAIN,
                        ActionRow.of(categoryMenu),
                        ActionRow.of(valueMenu),
                        ActionRow.of(Button.danger("jawlah_stop", "إنـهـاء الـلـعـبـة 🛑"))
                ))
                .useComponentsV2(true).build()).queue();
    }

    @Getter @Setter
    private static class JawlahGame {
        private final long channelId;
        private final long organizerId;
        private String gameName;
        private String teamAName;
        private String teamBName;
        private int scoreA = 0;
        private int scoreB = 0;
        private boolean turnA = true;
        
        private String selectedCategory;
        private int selectedValue;
        
        private final Set<String> usedQuestions = new HashSet<>();

        public JawlahGame(long channelId, long organizerId) {
            this.channelId = channelId;
            this.organizerId = organizerId;
        }
    }
}
