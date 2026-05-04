package com.integrafty.opexy.service.event;

import com.integrafty.opexy.utils.EmbedUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.modals.Modal;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class JawlahManager extends ListenerAdapter {

    private final JDA jda;
    private final EventManager eventManager;
    private final Map<Long, JawlahGame> activeGames = new HashMap<>();
    private final Map<String, List<JawlahQuestion>> questionBank = new HashMap<>();

    @PostConstruct
    public void init() {
        jda.addEventListener(this);
        loadQuestions();
    }

    private void loadQuestions() {
        // Football
        addQ("football_player_eye", new JawlahQuestion("من هذا اللاعب؟", "ميسي", "https://i.imgur.com/8mY2PZ0.png"));
        addQ("football_player_eye", new JawlahQuestion("من صاحب هذه العين؟", "رونالدو", "https://i.imgur.com/r6TqB7C.png"));
        addQ("football_who_player", new JawlahQuestion("لاعب برازيلي لعب لبرشلونة وميلان وفاز بالكرة الذهبية 2005", "رونالدينيو", null));
        addQ("football_who_player", new JawlahQuestion("الهداف التاريخي لبطولة كأس العالم", "كلوزه", null));
        
        // World
        addQ("world_flags", new JawlahQuestion("عاصمة هذه الدولة؟", "الرياض", "https://i.imgur.com/S8Wn9Z2.png"));
        addQ("world_capitals", new JawlahQuestion("ما هي عاصمة اليابان؟", "طوكيو", null));
        addQ("world_capitals", new JawlahQuestion("ما هي عاصمة فرنسا؟", "باريس", null));
        
        // General
        addQ("general_animals", new JawlahQuestion("ما هو أسرع حيوان بري؟", "الفهد", null));
        addQ("general_general_info", new JawlahQuestion("كم عدد كواكب المجموعة الشمسية؟", "8", null));
        addQ("general_complete_proverb", new JawlahQuestion("أكمل المثل: الوقت كالسيف إن لم تقطعه...", "قطعك", null));
        
        // Islamic
        addQ("islamic_prophets", new JawlahQuestion("من هو النبي الملقب بكليم الله؟", "موسى", null));
        addQ("islamic_quran", new JawlahQuestion("ما هي أطول سورة في القرآن الكريم؟", "البقرة", null));
        
        // Art
        addQ("art_one_piece", new JawlahQuestion("من هو بطل قصة ون بيس؟", "لوفي", null));
        addQ("art_breaking_bad", new JawlahQuestion("ما هو الاسم المستعار لوالتر وايت؟", "هايزنبرغ", null));
        
        // Add more samples as needed...
    }

    private void addQ(String key, JawlahQuestion q) {
        questionBank.computeIfAbsent(key, k -> new ArrayList<>()).add(q);
    }

    public void initiateSetup(net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent event) {
        Modal modal = Modal.create("jawlah_setup_modal", "إعـــدادات لـــعـــبـــة جـــولـــة")
                .addComponents(
                        Label.of("اســـم الـــلـــعـــبـــة", TextInput.create("game_name", TextInputStyle.SHORT).setRequired(true).setPlaceholder("مـــثـــال: تـــحـــدي الـــعـــمـــالـــقـــة").build()),
                        Label.of("اســـم الـــفـــريـــق الأول", TextInput.create("team_a_name", TextInputStyle.SHORT).setRequired(true).setPlaceholder("مـــثـــال: فـــريـــق الـــصـــقـــور").build()),
                        Label.of("اســـم الـــفـــريـــق الـــثـــانـــي", TextInput.create("team_b_name", TextInputStyle.SHORT).setRequired(true).setPlaceholder("مـــثـــال: فـــريـــق الـــنـــســـور").build()),
                        Label.of("عـدد الـلاعـبـيـن لـكـل فـريـق", TextInput.create("max_players", TextInputStyle.SHORT).setRequired(true).setPlaceholder("مـــثـــال: 3").build())
                ).build();
        event.replyModal(modal).queue();
    }

    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        if (!event.getModalId().equals("jawlah_setup_modal")) return;

        String gameName = event.getValue("game_name").getAsString();
        String teamA = event.getValue("team_a_name").getAsString();
        String teamB = event.getValue("team_b_name").getAsString();
        int maxPlayers = 5;
        try { maxPlayers = Integer.parseInt(event.getValue("max_players").getAsString()); } catch (Exception ignored) {}

        JawlahGame game = new JawlahGame(event.getChannel().getIdLong(), event.getUser().getIdLong());
        game.setGameName(gameName);
        game.setTeamAName(teamA);
        game.setTeamBName(teamB);
        game.setMaxPlayersPerTeam(maxPlayers);
        activeGames.put(event.getChannel().getIdLong(), game);

        sendHelpingHandsSelection(event);
    }

    private void sendHelpingHandsSelection(net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback event) {
        JawlahGame game = activeGames.get(event.getChannel().getIdLong());
        
        String enabledStr = game.getEnabledHelpers().isEmpty() ? "*لا يوجد*" : String.join(" - ", game.getEnabledHelpers());
        
        StringBuilder playersList = new StringBuilder();
        playersList.append(String.format("🔵 **%s** (%d/%d):\n", game.teamAName, game.teamAPlayers.size(), game.maxPlayersPerTeam));
        if (game.teamAPlayers.isEmpty()) playersList.append("*في انتظار اللاعبين...*\n");
        else game.teamAPlayers.forEach(id -> playersList.append("<@").append(id).append("> "));
        
        playersList.append(String.format("\n\n🔴 **%s** (%d/%d):\n", game.teamBName, game.teamBPlayers.size(), game.maxPlayersPerTeam));
        if (game.teamBPlayers.isEmpty()) playersList.append("*في انتظار اللاعبين...*\n");
        else game.teamBPlayers.forEach(id -> playersList.append("<@").append(id).append("> "));

        String body = "### 🛠️ إعـــدادات الـــلـــعـــبـــة\n" +
                playersList.toString() + "\n\n" +
                "**الـــمـــســـاعـــدات الـــمـــفـــعـــلـــه:**\n" +
                enabledStr + "\n\n" +
                "اخـــتـــر فـــريـــقـــك لـــلـــدخول، والـــمـــنـــظـــم يـــمـــكـــنـــه تـــفـــعـــيـــل الـــمـــســـاعـــدات وبـــدء الـــلـــعـــب.";
        
        Button joinA = Button.primary("jawlah_join_a", "انضمام لـ " + game.teamAName + " 🔵")
                .withDisabled(game.teamAPlayers.size() >= game.maxPlayersPerTeam);
        Button joinB = Button.danger("jawlah_join_b", "انضمام لـ " + game.teamBName + " 🔴")
                .withDisabled(game.teamBPlayers.size() >= game.maxPlayersPerTeam);

        net.dv8tion.jda.api.components.container.Container container = EmbedUtil.containerBranded("SETUP", "تـــجـــهـــيـــز الـــفـــرق والـــمـــســـاعـــدات", body, EmbedUtil.BANNER_MAIN,
                        ActionRow.of(joinA, joinB),
                        ActionRow.of(
                                Button.secondary("jawlah_help_1", "جاوب جوابين ✌️"),
                                Button.secondary("jawlah_help_3", "الحفرة ⛳"),
                                Button.secondary("jawlah_help_4", "اعكس الدور 🔄"),
                                Button.secondary("jawlah_help_5", "السؤال الذهبي 🏆")
                        ),
                        ActionRow.of(Button.success("jawlah_start_confirm", "بـــدء الـــلـــعـــب 🚀"))
                );

        if (event instanceof ButtonInteractionEvent bie) {
            bie.editMessage(new MessageEditBuilder().setComponents(container).useComponentsV2(true).build()).queue();
        } else if (event instanceof ModalInteractionEvent mie) {
            mie.reply(new MessageCreateBuilder().setComponents(container).useComponentsV2(true).build()).queue();
        }
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
            sendSubCategoryMenu(event, game);
        } else if (id.equals("jawlah_subcategory")) {
            game.setSelectedSubCategory(event.getValues().get(0));
            event.reply("تم اختيار الفئة: **" + game.getSelectedSubCategory() + "**. الآن اختر القيمة من اللوحة الرئيسية.").setEphemeral(true).queue();
        } else if (id.equals("jawlah_value")) {
            int val = Integer.parseInt(event.getValues().get(0));
            String key = game.getSelectedCategory() + "_" + val;
            
            if (game.getUsedQuestions().contains(key)) {
                event.reply("⚠️ هذا السؤال تم استخدامه بالفعل! اختر قيمة أخرى.").setEphemeral(true).queue();
                return;
            }
            
            game.setSelectedValue(val);
            game.getUsedQuestions().add(key);
            showQuestionPrompt(event, game);
        }
    }

    private void sendSubCategoryMenu(StringSelectInteractionEvent event, JawlahGame game) {
        String cat = game.getSelectedCategory();
        StringSelectMenu.Builder menu = StringSelectMenu.create("jawlah_subcategory").setPlaceholder("اخـتـر الـفـئـة الـفـرعـيـة...");

        switch (cat) {
            case "football" -> {
                menu.addOption("عين اللاعب 👀", "player_eye");
                menu.addOption("من اللاعب 👤", "who_player");
                menu.addOption("لاعبين صغار 👶", "young_players");
                menu.addOption("شعارات أندية 🛡️", "club_logos");
                menu.addOption("اسم اللاعب 📛", "player_name");
            }
            case "guess" -> {
                menu.addOption("خمن الأكل 🍔", "guess_food");
                menu.addOption("خمن الاسم 👤", "guess_name");
                menu.addOption("خمن الصورة 🖼️", "guess_img");
                menu.addOption("خمن اللاعب ⚽", "guess_player");
            }
            case "world" -> {
                menu.addOption("لغات 🗣️", "langs");
                menu.addOption("أعلام 🚩", "flags");
                menu.addOption("عملات ورقية 💵", "banknotes");
                menu.addOption("رمز العملة 💱", "currency_sym");
                menu.addOption("خرائط 🗺️", "maps");
                menu.addOption("عواصم 🏛️", "capitals");
                menu.addOption("لون العلم 🎨", "flag_colors");
            }
            case "general" -> {
                menu.addOption("عالم الحيوان 🦁", "animals");
                menu.addOption("جيل الطيبين 📺", "old_gen");
                menu.addOption("ولا كلمة 🙊", "no_word");
                menu.addOption("معلومات عامة 💡", "general_info");
                menu.addOption("أكمل المثل 📝", "complete_proverb");
                menu.addOption("وش الأكلة 🥘", "what_food");
                menu.addOption("الشعار الصحيح ✅", "correct_logo");
                menu.addOption("كودنيمز 🧩", "codenames");
                menu.addOption("صور ألغاز 🧩", "puzzle_imgs");
                menu.addOption("شعارات 🏷️", "logos");
                menu.addOption("حروف 🅰️", "letters");
                menu.addOption("شعار السيارة 🚗", "car_logo");
                menu.addOption("تغبيشة 🌫️", "blur");
                menu.addOption("٣ صور 🖼️", "three_imgs");
            }
            case "islamic" -> {
                menu.addOption("القرآن 📖", "quran");
                menu.addOption("جزء تبارك 📑", "tabarak");
                menu.addOption("إسلامي 🕌", "islamic_general");
                menu.addOption("جزء عم 📜", "amma");
                menu.addOption("قصص أنبياء ✨", "prophets");
            }
            case "art" -> {
                menu.addOption("One Piece 👒", "one_piece");
                menu.addOption("Game of Thrones ⚔️", "got");
                menu.addOption("شخصيات كرتونية 🐭", "cartoons");
                menu.addOption("Breaking Bad 🧪", "breaking_bad");
            }
        }

        event.editMessage(new MessageEditBuilder()
                .setComponents(ActionRow.of(menu.build()), ActionRow.of(Button.secondary("jawlah_back", "الـعـودة لـلـوحـة ⬅️")))
                .build()).queue();
    }

    private void showQuestionPrompt(net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback event, JawlahGame game) {
        String key = game.selectedCategory + "_" + game.selectedSubCategory;
        List<JawlahQuestion> qs = questionBank.getOrDefault(key, questionBank.get("general_general_info"));
        JawlahQuestion q = qs.get(new Random().nextInt(qs.size()));
        game.setCurrentQuestion(q);
        game.setAttemptsLeft(game.getEnabledHelpers().contains("جاوب جوابين ✌️") ? 2 : 1);

        String modifiers = (game.isPitActive() ? "⛳ **الـحـفـرة مـفـعـلـة**\n" : "") +
                           (game.isGoldenQuestion() ? "🏆 **الـسـؤال الـذهـبـي مـفـعـل**\n" : "");

        String body = String.format("### ❓ ســـؤال الـــتـــحـــدي\n\n" +
                "**الـفـئـة:** `%s` -> `%s`\n" +
                "**الـقـيـمـة:** `%d` نـقـطـة\n" +
                "**الـدور لـفـريـق:** %s\n\n" +
                "📢 **الـسـؤال:** %s\n\n" +
                "%s" +
                "يـرجـى كـتـابـة الإجـابـة فـي الـشـات الآن!",
                game.selectedCategory, game.selectedSubCategory, game.selectedValue, 
                game.turnA ? "🔵 " + game.teamAName : "🔴 " + game.teamBName, q.text, modifiers);

        MessageEditBuilder edit = new MessageEditBuilder()
                .setComponents(EmbedUtil.containerBranded("QUESTION", "🔍 جـــاري الـــتـــحـــدي...", body, q.imageUrl != null ? q.imageUrl : EmbedUtil.BANNER_MAIN,
                        ActionRow.of(
                                Button.secondary("jawlah_help_1", "جاوب جوابين ✌️"),
                                Button.secondary("jawlah_help_4", "اعكس الدور 🔄"),
                                Button.secondary("jawlah_help_5", "السؤال الذهبي 🏆")
                        ),
                        ActionRow.of(
                                Button.secondary("jawlah_back", "الـعـودة لـلـوحـة ⬅️")
                        )
                ))
                .useComponentsV2(true);

        if (event instanceof ButtonInteractionEvent bie) bie.editMessage(edit.build()).queue();
        else if (event instanceof StringSelectInteractionEvent ssie) ssie.editMessage(edit.build()).queue();
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
            case "jawlah_back" -> sendBoard(event);
            case "jawlah_start_confirm" -> sendBoard(event);
            case "jawlah_stop" -> {
                activeGames.remove(event.getChannel().getIdLong());
                eventManager.endGroupEvent();
                event.reply("🛑 تم إنهاء لعبة جولة.").queue();
            }
            case "jawlah_join_a" -> {
                long uid = event.getUser().getIdLong();
                if (game.teamAPlayers.contains(uid)) {
                    event.reply("⚠️ أنت بالفعل في الفريق الأول!").setEphemeral(true).queue();
                    return;
                }
                game.teamBPlayers.remove(uid);
                if (game.teamAPlayers.size() < game.maxPlayersPerTeam) {
                    game.teamAPlayers.add(uid);
                    sendHelpingHandsSelection(event);
                } else {
                    event.reply("❌ عذراً، الفريق الأول ممتلئ!").setEphemeral(true).queue();
                }
            }
            case "jawlah_join_b" -> {
                long uid = event.getUser().getIdLong();
                if (game.teamBPlayers.contains(uid)) {
                    event.reply("⚠️ أنت بالفعل في الفريق الثاني!").setEphemeral(true).queue();
                    return;
                }
                game.teamAPlayers.remove(uid);
                if (game.teamBPlayers.size() < game.maxPlayersPerTeam) {
                    game.teamBPlayers.add(uid);
                    sendHelpingHandsSelection(event);
                } else {
                    event.reply("❌ عذراً، الفريق الثاني ممتلئ!").setEphemeral(true).queue();
                }
            }
            case "jawlah_help_1" -> {
                if (event.getUser().getIdLong() != game.organizerId) {
                    event.reply("❌ عذراً، المنظم فقط يمكنه تفعيل المساعدات.").setEphemeral(true).queue();
                    return;
                }
                game.getEnabledHelpers().add("جاوب جوابين ✌️");
                if (game.selectedValue > 0) showQuestionPrompt(event, game);
                else sendHelpingHandsSelection(event);
            }
            case "jawlah_help_3" -> {
                if (event.getUser().getIdLong() != game.organizerId) {
                    event.reply("❌ عذراً، المنظم فقط يمكنه تفعيل المساعدات.").setEphemeral(true).queue();
                    return;
                }
                game.setPitActive(true);
                game.getEnabledHelpers().add("الحفرة ⛳");
                if (game.selectedValue > 0) showQuestionPrompt(event, game);
                else sendHelpingHandsSelection(event);
            }
            case "jawlah_help_4" -> {
                if (event.getUser().getIdLong() != game.organizerId) {
                    event.reply("❌ عذراً، المنظم فقط يمكنه تفعيل المساعدات.").setEphemeral(true).queue();
                    return;
                }
                game.setTurnA(!game.turnA);
                game.getEnabledHelpers().add("اعكس الدور 🔄");
                if (game.selectedValue > 0) showQuestionPrompt(event, game);
                else sendHelpingHandsSelection(event);
            }
            case "jawlah_help_5" -> {
                if (event.getUser().getIdLong() != game.organizerId) {
                    event.reply("❌ عذراً، المنظم فقط يمكنه تفعيل المساعدات.").setEphemeral(true).queue();
                    return;
                }
                game.setGoldenQuestion(true);
                game.getEnabledHelpers().add("السؤال الذهبي 🏆");
                if (game.selectedValue > 0) showQuestionPrompt(event, game);
                else sendHelpingHandsSelection(event);
            }
        }
    }

        MessageEditBuilder edit = new MessageEditBuilder()
                .setComponents(EmbedUtil.containerBranded("GAME", "🎮 لوحـة الـتـحـدي — Jawlah Board", body, EmbedUtil.BANNER_MAIN,
                        ActionRow.of(categoryMenu),
                        ActionRow.of(valueMenu),
                        ActionRow.of(Button.danger("jawlah_stop", "إنـهـاء الـلـعـبـة 🛑"))
                ))
                .useComponentsV2(true);

        event.editMessage(edit.build()).queue(m -> game.setBoardMessageId(m.getIdLong()));
    }

    private void sendBoard(net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback event) {
        JawlahGame game = activeGames.get(event.getChannel().getIdLong());
        game.setSelectedValue(0); 
        
        int totalQuestions = 6 * 3; 
        int usedCount = game.getUsedQuestions().size();
        int remaining = totalQuestions - usedCount;

        String modifiers = (game.isPitActive() ? "⛳ **الـحـفـرة مـفـعـلـة**\n" : "") +
                           (game.isGoldenQuestion() ? "🏆 **الـسـؤال الـذهـبـي مـفـعـل**\n" : "");

        String body = String.format("### 🏆 %s\n\n" +
                "**الـنـتـيـجـة:**\n" +
                "🔵 **%s**: `%d` نـقـطـة\n" +
                "🔴 **%s**: `%d` نـقـطـة\n\n" +
                "📊 **الأســـئـــلـــة الـــمـــتـــبـــقـــيـــة:** `%d / %d`\n\n" +
                "%s" +
                "**الـدور الـحـالـي:** %s\n\n" +
                "يـرجـى اخـتـيـار الـفـئـة والـقـيـمـة مـن الـقـوائـم أدناه.",
                game.gameName, game.teamAName, game.scoreA, game.teamBName, game.scoreB, 
                remaining, totalQuestions, modifiers, game.turnA ? "🔵 " + game.teamAName : "🔴 " + game.teamBName);

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
                .setPlaceholder("اخـتـر الـقـيـمـة (300, 600, 900)...")
                .addOption("300 نـقـطـة", "300")
                .addOption("600 نـقـطـة", "600")
                .addOption("900 نـقـطـة", "900")
                .build();

        MessageEditBuilder edit = new MessageEditBuilder()
                .setComponents(EmbedUtil.containerBranded("GAME", "🎮 لوحـة الـتـحـدي — Jawlah Board", body, EmbedUtil.BANNER_MAIN,
                        ActionRow.of(categoryMenu),
                        ActionRow.of(valueMenu),
                        ActionRow.of(Button.danger("jawlah_stop", "إنـهـاء الـلـعـبـة 🛑"))
                ))
                .useComponentsV2(true);

        event.editMessage(edit.build()).queue(m -> game.setBoardMessageId(m.getIdLong()));
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;
        JawlahGame game = activeGames.get(event.getChannel().getIdLong());
        if (game == null || game.getCurrentQuestion() == null) return;

        long uid = event.getAuthor().getIdLong();
        boolean isA = game.teamAPlayers.contains(uid);
        boolean isB = game.teamBPlayers.contains(uid);

        if (!isA && !isB && uid != game.organizerId) return;

        // Turn check
        if ((game.turnA && !isA) || (!game.turnA && !isB)) {
            // Optional: inform that it's not their turn
            return;
        }

        String content = event.getMessage().getContentRaw().trim();
        String answer = game.getCurrentQuestion().answer;

        if (isSimilar(content, answer)) {
            // Correct!
            int points = game.selectedValue;
            if (game.isGoldenQuestion()) points *= 2;

            if (game.turnA) {
                game.scoreA += points;
                if (game.isPitActive()) game.scoreB = Math.max(0, game.scoreB - points);
            } else {
                game.scoreB += points;
                if (game.isPitActive()) game.scoreA = Math.max(0, game.scoreA - points);
            }

            event.getMessage().reply("✅ إجابة صحيحة! أحسنت.").queue();
            game.setCurrentQuestion(null);
            game.setGoldenQuestion(false);
            game.setPitActive(false);
            game.setTurnA(!game.turnA);
            
            refreshBoard(game);
        } else {
            // Wrong
            game.setAttemptsLeft(game.getAttemptsLeft() - 1);
            if (game.attemptsLeft <= 0) {
                event.getMessage().reply("❌ إجابة خاطئة! الإجابة الصحيحة كانت: **" + answer + "**").queue();
                game.setCurrentQuestion(null);
                game.setGoldenQuestion(false);
                game.setPitActive(false);
                game.setTurnA(!game.turnA);
                refreshBoard(game);
            } else {
                event.getMessage().reply("❌ إجابة خاطئة! لديك محاولة واحدة متبقية...").queue();
            }
        }
    }

    private boolean isSimilar(String input, String target) {
        if (input == null || target == null) return false;
        return input.equalsIgnoreCase(target) || 
               input.replaceAll("\\s+", "").equalsIgnoreCase(target.replaceAll("\\s+", ""));
    }

    private void refreshBoard(JawlahGame game) {
        if (game.getBoardMessageId() == 0) return;
        
        int totalQuestions = 6 * 3; 
        int usedCount = game.getUsedQuestions().size();
        int remaining = totalQuestions - usedCount;

        String modifiers = (game.isPitActive() ? "⛳ **الـحـفـرة مـفـعـلـة**\n" : "") +
                           (game.isGoldenQuestion() ? "🏆 **الـسـؤال الـذهـبـي مـفـعـل**\n" : "");

        String body = String.format("### 🏆 %s\n\n" +
                "**الـنـتـيـجـة:**\n" +
                "🔵 **%s**: `%d` نـقـطـة\n" +
                "🔴 **%s**: `%d` نـقـطـة\n\n" +
                "📊 **الأســـئـــلـــة الـــمـــتـــبـــقـــيـــة:** `%d / %d`\n\n" +
                "%s" +
                "**الـدور الـحـالـي:** %s\n\n" +
                "يـرجـى اخـتـيـار الـفـئـة والـقـيـمـة مـن الـقـوائـم أدناه.",
                game.gameName, game.teamAName, game.scoreA, game.teamBName, game.scoreB, 
                remaining, totalQuestions, modifiers, game.turnA ? "🔵 " + game.teamAName : "🔴 " + game.teamBName);

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
                .setPlaceholder("اخـتـر الـقـيـمـة (300, 600, 900)...")
                .addOption("300 نـقـطـة", "300")
                .addOption("600 نـقـطـة", "600")
                .addOption("900 نـقـطـة", "900")
                .build();

        MessageEditBuilder edit = new MessageEditBuilder()
                .setComponents(EmbedUtil.containerBranded("GAME", "🎮 لوحـة الـتـحـدي — Jawlah Board", body, EmbedUtil.BANNER_MAIN,
                        ActionRow.of(categoryMenu),
                        ActionRow.of(valueMenu),
                        ActionRow.of(Button.danger("jawlah_stop", "إنـهـاء الـلـعـبـة 🛑"))
                ))
                .useComponentsV2(true);

        jda.getTextChannelById(game.getChannelId()).editMessageById(game.getBoardMessageId(), edit.build())
            .queue(null, e -> {});
    }

    @Getter @Setter
    private static class JawlahQuestion {
        private final String text;
        private final String answer;
        private final String imageUrl;

        public JawlahQuestion(String text, String answer, String imageUrl) {
            this.text = text;
            this.answer = answer;
            this.imageUrl = imageUrl;
        }
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
        private String selectedSubCategory;
        private int selectedValue;

        private boolean pitActive = false;
        private boolean goldenQuestion = false;
        
        private int maxPlayersPerTeam = 5;
        private final Set<Long> teamAPlayers = new LinkedHashSet<>();
        private final Set<Long> teamBPlayers = new LinkedHashSet<>();
        
        private final Set<String> usedQuestions = new HashSet<>();
        private final Set<String> enabledHelpers = new LinkedHashSet<>();
        
        private JawlahQuestion currentQuestion;
        private int attemptsLeft = 1;
        private long boardMessageId;

        public JawlahGame(long channelId, long organizerId) {
            this.channelId = channelId;
            this.organizerId = organizerId;
        }
    }
}
