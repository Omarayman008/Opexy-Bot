package com.integrafty.opexy.command;

import com.integrafty.opexy.command.base.MultiSlashCommand;
import com.integrafty.opexy.entity.UserEntity;
import com.integrafty.opexy.repository.UserRepository;
import com.integrafty.opexy.utils.EmbedUtil;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class EventPointCommands implements MultiSlashCommand {

    private final UserRepository userRepository;

    @Override
    public List<SlashCommandData> getCommandDataList() {
        List<SlashCommandData> list = new ArrayList<>();

        list.add(Commands.slash("add-point", "إضـــافـــة نـــقـــاط فـــعـــالـــيـــات لـــعـــضـــو")
                .addOption(OptionType.INTEGER, "amount", "عـــدد الـــنـــقـــاط", true)
                .addOption(OptionType.USER, "user", "الـــعـــضـــو الـــمـــســـتـــهـــدف", true)
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.KICK_MEMBERS)));

        list.add(Commands.slash("remove-point", "ســـحـــب نـــقـــاط فـــعـــالـــيـــات مـــن عـــضـــو")
                .addOption(OptionType.INTEGER, "amount", "عـــدد الـــنـــقـــاط", true)
                .addOption(OptionType.USER, "user", "الـــعـــضـــو الـــمـــســـتـــهـــدف", true)
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.KICK_MEMBERS)));

        list.add(Commands.slash("points", "عـــرض نـــقـــاط الـــفـــعـــالـــيـــات لـــعـــضـــو مـــعـــيـــن")
                .addOption(OptionType.USER, "user", "الـــعـــضـــو (اخـــتـــيـــاري)", false));

        list.add(Commands.slash("points-list", "عـــرض قـــائـــمـــة مـــتـــصـــدري نـــقـــاط الـــفـــعـــالـــيـــات"));

        return list;
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        String name = event.getName();

        switch (name) {
            case "add-point" -> handleAddPoint(event);
            case "remove-point" -> handleRemovePoint(event);
            case "points" -> handlePoints(event);
            case "points-list" -> handlePointsList(event);
        }
    }

    private void handleAddPoint(SlashCommandInteractionEvent event) {
        int amount = event.getOption("amount").getAsInt();
        Member target = event.getOption("user").getAsMember();
        if (target == null) return;

        UserEntity user = getOrCreateUser(target.getId(), event.getGuild().getId());
        user.setEventPoints(user.getEventPoints() + amount);
        userRepository.save(user);

        String body = String.format("### ✅ تـــم إضـــافـــة نـــقـــاط\n▫️ **الـــعـــضـــو:** %s\n▫️ **الـــمـــبـــلـــغ:** `%d` نـــقـــطـــة\n▫️ **الـــرصـــيـــد الـــحـــالـــي:** `%d`",
                target.getAsMention(), amount, user.getEventPoints());
        
        event.reply(new MessageCreateBuilder()
                .setComponents(EmbedUtil.containerBranded("EVENTS", "Points Update", body, EmbedUtil.BANNER_MAIN))
                .useComponentsV2(true).build()).queue();
    }

    private void handleRemovePoint(SlashCommandInteractionEvent event) {
        int amount = event.getOption("amount").getAsInt();
        Member target = event.getOption("user").getAsMember();
        if (target == null) return;

        UserEntity user = getOrCreateUser(target.getId(), event.getGuild().getId());
        user.setEventPoints(Math.max(0, user.getEventPoints() - amount));
        userRepository.save(user);

        String body = String.format("### ❌ تـــم ســـحـــب نـــقـــاط\n▫️ **الـــعـــضـــو:** %s\n▫️ **الـــمـــبـــلـــغ:** `%d` نـــقـــطـــة\n▫️ **الـــرصـــيـــد الـــحـــالـــي:** `%d`",
                target.getAsMention(), amount, user.getEventPoints());
        
        event.reply(new MessageCreateBuilder()
                .setComponents(EmbedUtil.containerBranded("EVENTS", "Points Update", body, EmbedUtil.BANNER_MAIN))
                .useComponentsV2(true).build()).queue();
    }

    private void handlePoints(SlashCommandInteractionEvent event) {
        Member target = event.getOption("user", OptionMapping::getAsMember);
        if (target == null) target = event.getMember();

        UserEntity user = getOrCreateUser(target.getId(), event.getGuild().getId());

        String body = String.format("### 📊 نـــقـــاط الـــفـــعـــالـــيـــات\n▫️ **الـــعـــضـــو:** %s\n▫️ **الـــنـــقـــاط الـــحـــالـــيـــة:** `%d`",
                target.getAsMention(), user.getEventPoints());

        event.reply(new MessageCreateBuilder()
                .setComponents(EmbedUtil.containerBranded("EVENTS", "Points Status", body, EmbedUtil.BANNER_MAIN))
                .useComponentsV2(true).build()).queue();
    }

    private void handlePointsList(SlashCommandInteractionEvent event) {
        List<UserEntity> top = userRepository.findTop10ByGuildIdOrderByEventPointsDesc(event.getGuild().getId());
        
        StringBuilder sb = new StringBuilder();
        sb.append("### 🏆 مـــتـــصـــدرو نـــقـــاط الـــفـــعـــالـــيـــات\n\n");
        
        if (top.isEmpty()) {
            sb.append("*لا يـــوجـــد بـــيـــانـــات حـــالـــيـــاً.*");
        } else {
            for (int i = 0; i < top.size(); i++) {
                UserEntity u = top.get(i);
                sb.append(String.format("`#%d` <@%s> — **%d** نـــقـــطـــة\n", i + 1, u.getUserId(), u.getEventPoints()));
            }
        }

        event.reply(new MessageCreateBuilder()
                .setComponents(EmbedUtil.containerBranded("EVENTS", "Leaderboard", sb.toString(), EmbedUtil.BANNER_MAIN))
                .useComponentsV2(true).build()).queue();
    }

    private UserEntity getOrCreateUser(String userId, String guildId) {
        return userRepository.findByUserIdAndGuildId(userId, guildId)
                .orElse(new UserEntity(userId, guildId, 0, 0, false, null, null, 0, 0));
    }
}
