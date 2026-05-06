package com.integrafty.opexy.service.notification;

import com.google.gson.JsonObject;
import com.integrafty.opexy.entity.NotificationEntity;
import com.integrafty.opexy.repository.NotificationRepository;
import com.integrafty.opexy.utils.EmbedUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.container.Container;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationScheduler {

    private final JDA jda;
    private final NotificationRepository notificationRepository;
    private final KickService kickService;
    private final TwitchService twitchService;
    private final YouTubeService youtubeService;

    private static final String LIVE_CHANNEL_ID = "1487140470172815574";
    private static final String VIDEO_CHANNEL_ID = "1487204004076191946";
    private static final String LIVE_ROLE_MENTION = "<@&1487196786488770610>";
    private static final String VIDEO_ROLE_MENTION = "<@&1500269236583399454>";

    @Scheduled(fixedRate = 60000) // 1 Minute
    @org.springframework.transaction.annotation.Transactional
    public void checkNotifications() {
        log.info("Starting notification check cycle...");
        List<NotificationEntity> entities = notificationRepository.findAll();
        for (NotificationEntity entity : entities) {
            if (!entity.isActive()) continue;

            try {
                switch (entity.getPlatform().toUpperCase()) {
                    case "KICK":
                        handleKick(entity);
                        break;
                    case "TWITCH":
                        handleTwitch(entity);
                        break;
                    case "YOUTUBE":
                        handleYouTube(entity);
                        break;
                }
            } catch (Exception e) {
                log.error("Error checking notification for {}: {}", entity.getDisplayName(), e.getMessage());
            }
        }
    }

    private void handleKick(NotificationEntity entity) {
        kickService.getStreamStatus(entity.getChannelId()).ifPresent(livestream -> {
            String streamId = livestream.get("id").getAsString();
            if (entity.getLastContentId() == null) {
                // First run - set baseline
                entity.setLastContentId(streamId);
                notificationRepository.save(entity);
                return;
            }
            if (!streamId.equals(entity.getLastContentId())) {
                String title = livestream.get("session_title").getAsString();
                String thumbnail = livestream.getAsJsonObject("thumbnail").get("url").getAsString();
                String url = "https://kick.com/" + entity.getChannelId();
                sendLiveNotification(entity, streamId, url, title, thumbnail, "KICK");
            }
        });
    }

    private void handleTwitch(NotificationEntity entity) {
        twitchService.getStreamStatus(entity.getChannelId()).ifPresent(json -> {
            String streamId = json.get("id").getAsString();
            if (entity.getLastContentId() == null) {
                entity.setLastContentId(streamId);
                notificationRepository.save(entity);
                return;
            }
            if (!streamId.equals(entity.getLastContentId())) {
                String title = json.get("title").getAsString();
                String thumbnail = json.get("thumbnail_url").getAsString().replace("{width}", "1280").replace("{height}", "720");
                String url = "https://twitch.tv/" + entity.getChannelId();
                sendLiveNotification(entity, streamId, url, title, thumbnail, "TWITCH");
            }
        });
    }

    private void handleYouTube(NotificationEntity entity) {
        if (entity.getChannelId() != null && !entity.getChannelId().startsWith("UC")) {
            String resolved = youtubeService.resolveChannelId(entity.getChannelId());
            if (resolved != null && resolved.startsWith("UC")) {
                entity.setChannelId(resolved);
                notificationRepository.save(entity);
            } else {
                return;
            }
        }

        youtubeService.getLatestVideo(entity.getChannelId()).ifPresentOrElse(json -> {
            String videoId = json.get("videoId").getAsString();
            if (entity.getLastContentId() == null) {
                // Baseline - don't notify for old videos
                entity.setLastContentId(videoId);
                notificationRepository.save(entity);
                log.info("Baseline set for YouTube {}: {}", entity.getDisplayName(), videoId);
                return;
            }
            if (!videoId.equals(entity.getLastContentId())) {
                String title = json.get("title").getAsString();
                String thumbnail = json.get("thumbnail").getAsString();
                String url = "https://youtube.com/watch?v=" + videoId;
                sendVideoNotification(entity, videoId, url, title, thumbnail);
            }
        }, () -> {
            youtubeService.scrapeLatestVideo(entity.getChannelId()).ifPresent(json -> {
                String videoId = json.get("videoId").getAsString();
                if (entity.getLastContentId() == null) {
                    entity.setLastContentId(videoId);
                    notificationRepository.save(entity);
                    return;
                }
                if (!videoId.equals(entity.getLastContentId())) {
                    String title = json.get("title").getAsString();
                    String thumbnail = json.get("thumbnail").getAsString();
                    String url = "https://youtube.com/watch?v=" + videoId;
                    sendVideoNotification(entity, videoId, url, title, thumbnail);
                }
            });
        });
    }

    private void sendLiveNotification(NotificationEntity entity, String contentId, String url, String title, String thumbnail, String platform) {
        TextChannel channel = jda.getTextChannelById(LIVE_CHANNEL_ID);
        if (channel == null) return;

        entity.setLastContentId(contentId);
        notificationRepository.save(entity);

        net.dv8tion.jda.api.EmbedBuilder eb = new net.dv8tion.jda.api.EmbedBuilder()
            .setImage(thumbnail)
            .setAuthor(platform + " ・ LIVE STREAM", null, null)
            .setTitle(title)
            .setDescription(LIVE_ROLE_MENTION + "\n\n### " + entity.getDisplayName() + " is Live now!\nJoin the broadcast via the link below.")
            .setColor(EmbedUtil.SUCCESS)
            .setFooter("\u25AA UNIFIED TERMINAL \u30FB HIGHCORE AGENCY \u30FB " + platform.toUpperCase() + " \u25AA");

        MessageCreateBuilder builder = new MessageCreateBuilder()
            .setContent(LIVE_ROLE_MENTION)
            .setEmbeds(eb.build())
            .setComponents(ActionRow.of(Button.link(url, "Watch Stream")));

        channel.sendMessage(builder.build()).queue();
    }

    private void sendVideoNotification(NotificationEntity entity, String contentId, String url, String title, String thumbnail) {
        TextChannel channel = jda.getTextChannelById(VIDEO_CHANNEL_ID);
        if (channel == null) return;

        entity.setLastContentId(contentId);
        notificationRepository.save(entity);

        net.dv8tion.jda.api.EmbedBuilder eb = new net.dv8tion.jda.api.EmbedBuilder()
            .setImage(thumbnail)
            .setAuthor("YOUTUBE ・ NEW UPLOAD", null, null)
            .setTitle(title)
            .setDescription(VIDEO_ROLE_MENTION + "\n\n### New Video from " + entity.getDisplayName() + "!\nCheck out the latest upload on the channel.")
            .setColor(EmbedUtil.ACCENT)
            .setFooter("\u25AA UNIFIED TERMINAL \u30FB HIGHCORE AGENCY \u25AA");

        MessageCreateBuilder builder = new MessageCreateBuilder()
            .setContent(VIDEO_ROLE_MENTION)
            .setEmbeds(eb.build())
            .setComponents(ActionRow.of(Button.link(url, "Watch Video")));

        channel.sendMessage(builder.build()).queue();
    }
}
