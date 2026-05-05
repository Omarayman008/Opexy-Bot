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
            if (!streamId.equals(entity.getLastContentId())) {
                String title = livestream.get("session_title").getAsString();
                String thumbnail = livestream.getAsJsonObject("thumbnail").get("url").getAsString();
                String url = "https://kick.com/" + entity.getChannelId();
                sendLiveNotification(entity, streamId, url, title, thumbnail, "KICK");
            }
        });
        if (!kickService.getStreamStatus(entity.getChannelId()).isPresent()) {
            entity.setLastContentId(null);
            notificationRepository.save(entity);
        }
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

        youtubeService.getLatestVideo(entity.getChannelId()).ifPresentOrElse(json -> {
            String videoId = json.get("videoId").getAsString();
            log.info("YouTube check for {}: latestVideoId={}, lastContentId={}", entity.getDisplayName(), videoId, entity.getLastContentId());
            if (!videoId.equals(entity.getLastContentId())) {
                String title = json.get("title").getAsString();
                String thumbnail = json.get("thumbnail").getAsString();
                String url = "https://youtube.com/watch?v=" + videoId;
                sendVideoNotification(entity, videoId, url, title, thumbnail);
            }
        }, () -> {
            log.info("YouTube check for {}: No videos found via RSS. Attempting fallback scrape...", entity.getDisplayName());
            youtubeService.scrapeLatestVideo(entity.getChannelId()).ifPresent(json -> {
                String videoId = json.get("videoId").getAsString();
                log.info("YouTube fallback scrape for {}: latestVideoId={}, lastContentId={}", entity.getDisplayName(), videoId, entity.getLastContentId());
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

        // Update state BEFORE queuing to prevent duplicates in next cycle
        entity.setLastContentId(contentId);
        notificationRepository.save(entity);

        String body = String.format("### %s is Live on %s!\n**%s**\n\nClick the button below to join the stream.", entity.getDisplayName(), platform, title);
        Container container = EmbedUtil.containerBranded(platform, "Live Stream", body, thumbnail, ActionRow.of(Button.link(url, "Watch Stream")));

        // Send ping first, then the rich message
        channel.sendMessage(LIVE_ROLE_MENTION).queue(ping -> {
            MessageCreateBuilder builder = new MessageCreateBuilder()
                .setComponents(container)
                .useComponentsV2(true);
            
            channel.sendMessage(builder.build())
                .useComponentsV2(true)
                .queue();
        });
    }

    private void sendVideoNotification(NotificationEntity entity, String contentId, String url, String title, String thumbnail) {
        TextChannel channel = jda.getTextChannelById(VIDEO_CHANNEL_ID);
        if (channel == null) {
            log.error("Video channel not found: {}", VIDEO_CHANNEL_ID);
            return;
        }

        log.info("Sending video notification for {} to channel {}", entity.getDisplayName(), VIDEO_CHANNEL_ID);
        
        // Update state BEFORE queuing to prevent duplicates in next cycle
        entity.setLastContentId(contentId);
        notificationRepository.save(entity);

        String body = String.format("### New Video from %s!\n**%s**\n\nClick the button below to watch the video.", entity.getDisplayName(), title);
        Container container = EmbedUtil.containerBranded("YOUTUBE", "New Upload", body, thumbnail, ActionRow.of(Button.link(url, "Watch Video")));

        // Send ping first, then the rich message
        channel.sendMessage(VIDEO_ROLE_MENTION).queue(ping -> {
            MessageCreateBuilder builder = new MessageCreateBuilder()
                .setComponents(container)
                .useComponentsV2(true);
            
            channel.sendMessage(builder.build())
                .useComponentsV2(true)
                .queue(msg -> log.info("Video notification sent successfully for {}", entity.getDisplayName()));
        });
    }
}
