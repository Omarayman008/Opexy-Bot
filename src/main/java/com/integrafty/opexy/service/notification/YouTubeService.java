package com.integrafty.opexy.service.notification;

import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class YouTubeService {

    private final RestTemplate restTemplate = new RestTemplate();

    public Optional<JsonObject> getLatestVideo(String channelId) {
        try {
            // YouTube RSS feed provides the latest videos without an API key
            String url = "https://www.youtube.com/feeds/videos.xml?channel_id=" + channelId;
            String xml = restTemplate.getForObject(url, String.class);
            
            if (xml != null && xml.contains("<entry>")) {
                JsonObject video = new JsonObject();
                video.addProperty("videoId", extractValue(xml, "<yt:videoId>(.*?)</yt:videoId>"));
                video.addProperty("title", extractValue(xml, "<title>(.*?)</title>"));
                video.addProperty("thumbnail", "https://i.ytimg.com/vi/" + video.get("videoId").getAsString() + "/maxresdefault.jpg");
                
                return Optional.of(video);
            }
        } catch (Exception e) {
            log.warn("YouTube RSS: Could not fetch videos for {}: {}", channelId, e.getMessage());
        }
        return Optional.empty();
    }

    private String extractValue(String xml, String regex) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(xml);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }
}
