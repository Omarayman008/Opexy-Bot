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

    @Scheduled(fixedRate = 300000) // 5 Minutes
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
        kickService.getStreamStatus(entity.getChannelId()).ifPresent(json -> {
            JsonObject livestream = json.has("livestream") && !json.get("livestream").isJsonNull() ? json.getAsJsonObject("livestream") : null;
            if (livestream != null) {
                String streamId = livestream.get("id").getAsString();
                if (!streamId.equals(entity.getLastContentId())) {
                    String title = livestream.get("session_title").getAsString();
                    String thumbnail = livestream.getAsJsonObject("thumbnail").get("url").getAsString();
                    String url = "https://kick.com/" + entity.getChannelId();
                    sendLiveNotification(entity, streamId, url, title, thumbnail, "KICK");
                }
            } else {
                entity.setLastContentId(null);
                notificationRepository.save(entity);
            }
        });
    }

    private void handleTwitch(NotificationEntity entity) {
        twitchService.getStreamStatus(entity.getChannelId()).ifPresent(json -> {
            String streamId = json.get("id").getAsString();
            if (!streamId.equals(entity.getLastContentId())) {
                String title = json.get("title").getAsString();
                String thumbnail = json.get("thumbnail_url").getAsString().replace("{width}", "1280").replace("{height}", "720");
                String url = "https://twitch.tv/" + entity.getChannelId();
                sendLiveNotification(entity, streamId, url, title, thumbnail, "TWITCH");
            }
        });
    }

    private void handleYouTube(NotificationEntity entity) {
        // AUTO-FIX: If ID is not a UC... ID, try to resolve it once and save for future cycles
        if (entity.getChannelId() != null && !entity.getChannelId().startsWith("UC")) {
            log.info("Attempting to resolve YouTube channel ID for {}", entity.getChannelId());
            String resolved = youtubeService.resolveChannelId(entity.getChannelId());
            if (resolved != null && resolved.startsWith("UC")) {
                entity.setChannelId(resolved);
                notificationRepository.save(entity);
                log.info("Resolved and saved YouTube channel ID for {}: {}", entity.getDisplayName(), resolved);
            } else {
                log.warn("Could not resolve YouTube channel ID for {}, skipping this cycle.", entity.getDisplayName());
                return;
            }
        }

        youtubeService.getLatestVideo(entity.getChannelId()).ifPresent(json -> {
            String videoId = json.get("videoId").getAsString();
            if (!videoId.equals(entity.getLastContentId())) {
                String title = json.get("title").getAsString();
                String thumbnail = json.get("thumbnail").getAsString();
                String url = "https://youtube.com/watch?v=" + videoId;
                sendVideoNotification(entity, videoId, url, title, thumbnail);
            }
        });
    }

    private void sendLiveNotification(NotificationEntity entity, String contentId, String url, String title, String thumbnail, String platform) {
        TextChannel channel = jda.getTextChannelById(LIVE_CHANNEL_ID);
        if (channel == null) return;

        String body = String.format("### %s is Live on %s!\n**%s**\n\nClick the button below to join the stream.", entity.getDisplayName(), platform, title);
        
        Container container = EmbedUtil.containerBranded(platform, "Live Stream", body, thumbnail, ActionRow.of(Button.link(url, "Watch Stream")));

        MessageCreateBuilder builder = new MessageCreateBuilder()
            .setContent("@everyone")
            .setComponents(container)
            .useComponentsV2(true);

        channel.sendMessage(builder.build()).useComponentsV2(true).queue(msg -> {
            entity.setLastContentId(contentId);
            notificationRepository.save(entity);
        });
    }

    private void sendVideoNotification(NotificationEntity entity, String contentId, String url, String title, String thumbnail) {
        TextChannel channel = jda.getTextChannelById(VIDEO_CHANNEL_ID);
        if (channel == null) return;

        String body = String.format("### New Video from %s!\n**%s**\n\nClick the button below to watch the video.", entity.getDisplayName(), title);
        
        Container container = EmbedUtil.containerBranded("YOUTUBE", "New Upload", body, thumbnail, ActionRow.of(Button.link(url, "Watch Video")));

        MessageCreateBuilder builder = new MessageCreateBuilder()
            .setContent("@everyone")
            .setComponents(container)
            .useComponentsV2(true);

        channel.sendMessage(builder.build()).useComponentsV2(true).queue(msg -> {
            entity.setLastContentId(contentId);
            notificationRepository.save(entity);
        });
    }
}
