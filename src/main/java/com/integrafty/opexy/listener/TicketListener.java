package com.integrafty.opexy.listener;

import com.integrafty.opexy.entity.TicketEntity;
import com.integrafty.opexy.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.JDA;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.awt.Color;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class TicketListener extends ListenerAdapter {

    private final JDA jda;
    private final TicketRepository ticketRepository;

    @PostConstruct
    public void init() {
        jda.addEventListener(this);
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String buttonId = event.getComponentId();

        if (buttonId.startsWith("ticket_") && !buttonId.equals("ticket_close")) {
            handleTicketCreation(event, buttonId);
        } else if (buttonId.equals("ticket_close")) {
            handleTicketClose(event);
        }
    }

    private void handleTicketCreation(ButtonInteractionEvent event, String buttonId) {
        String userId = event.getUser().getId();
        
        // Prevent creating multiple tickets
        if (ticketRepository.existsByUserIdAndStatus(userId, "OPEN")) {
            event.reply("❌ لديك تذكرة مفتوحة بالفعل! يرجى إغلاقها أولاً.").setEphemeral(true).queue();
            return;
        }

        String categoryName = "";
        String categoryId = buttonId.replace("ticket_", ""); // support, whitelist, etc
        Color embedColor = Color.WHITE;
        
        switch (categoryId) {
            case "support": 
                categoryName = "دعم-فني"; 
                embedColor = Color.decode("#5865F2"); 
                break;
            case "whitelist": 
                categoryName = "وايت-ليست"; 
                embedColor = Color.decode("#57F287"); 
                break;
            case "hiring": 
                categoryName = "توظيف"; 
                embedColor = Color.decode("#4F545C"); 
                break;
            case "complaint": 
                categoryName = "شكوى"; 
                embedColor = Color.decode("#ED4245"); 
                break;
        }

        Guild guild = event.getGuild();
        Member member = event.getMember();
        String channelName = categoryName + "-" + member.getUser().getName();

        final String finalCategoryName = categoryName;
        final Color finalEmbedColor = embedColor;

        // Create Text Channel
        guild.createTextChannel(channelName)
            .addPermissionOverride(guild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL))
            .addPermissionOverride(member, EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND), null)
            // Note: Admin roles are automatically granted access by Discord if they have Admin permission
            .queue(channel -> {
                // Save to DB
                TicketEntity ticket = new TicketEntity();
                ticket.setUserId(userId);
                ticket.setChannelId(channel.getId());
                ticket.setCategory(categoryId);
                ticketRepository.save(ticket);

                // V2 Premium Embed Styling
                EmbedBuilder embed = new EmbedBuilder()
                    .setAuthor(member.getUser().getAsTag(), null, member.getUser().getAvatarUrl())
                    .setTitle("🎫 تذكرة " + finalCategoryName.replace("-", " "))
                    .setDescription("مرحباً بك " + member.getAsMention() + ".\nيرجى طرح موضوعك أو مشكلتك بالتفصيل ليتمكن الفريق المختص من مساعدتك بشكل أسرع.")
                    .setColor(finalEmbedColor)
                    .setFooter("HighCore Tickets System", event.getJDA().getSelfUser().getAvatarUrl())
                    .setTimestamp(Instant.now());

                ActionRow buttons = ActionRow.of(
                    Button.danger("ticket_close", "🔒 إغلاق التذكرة")
                );

                // Mention user to ping them in the new channel, then delete the ping msg
                channel.sendMessage(member.getAsMention() + " 👑").queue(msg -> msg.delete().queueAfter(2, java.util.concurrent.TimeUnit.SECONDS));
                
                // Send the main embed
                channel.sendMessageEmbeds(embed.build()).setComponents(buttons).queue();

                event.reply("✅ تم إنشاء تذكرتك بنجاح: " + channel.getAsMention()).setEphemeral(true).queue();
            }, error -> {
                event.reply("❌ حدث خطأ أثناء إنشاء الغرفة، يرجى التأكد من صلاحيات البوت.").setEphemeral(true).queue();
                log.error("Error creating ticket channel", error);
            });
    }

    private void handleTicketClose(ButtonInteractionEvent event) {
        // Allow only admins or ticket creators to close
        boolean isAdmin = event.getMember().hasPermission(Permission.MANAGE_CHANNEL);
        
        TextChannel channel = event.getChannel().asTextChannel();
        Optional<TicketEntity> ticketOpt = ticketRepository.findByChannelId(channel.getId());
        
        if (ticketOpt.isPresent()) {
            TicketEntity ticket = ticketOpt.get();
            if (!isAdmin && !ticket.getUserId().equals(event.getUser().getId())) {
                event.reply("❌ لا تملك صلاحية لإغلاق هذه التذكرة.").setEphemeral(true).queue();
                return;
            }
            ticket.setStatus("CLOSED");
            ticketRepository.save(ticket);
        } else if (!isAdmin) {
             event.reply("❌ لا تملك صلاحية لإغلاق هذه التذكرة.").setEphemeral(true).queue();
             return;
        }

        event.reply("🔒 سيتم إغلاق التذكرة وحذف الغرفة نهائياً خلال 5 ثواني...").queue(success -> {
            channel.delete().queueAfter(5, java.util.concurrent.TimeUnit.SECONDS);
        });
    }
}
