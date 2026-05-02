package com.integrafty.opexy.service.notification;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;

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
            String resolvedId = channelId;
            
            if (!channelId.startsWith("UC")) {
                resolvedId = resolveChannelId(channelId);
            }

            String url = "https://www.youtube.com/feeds/videos.xml?channel_id=" + resolvedId;
            
            if (!resolvedId.startsWith("UC")) {
                url = "https://www.youtube.com/feeds/videos.xml?user=" + resolvedId.replace("@", "");
            }

            String xml = restTemplate.getForObject(url, String.class);
            
            if (xml != null && xml.contains("<entry>")) {
                int entryStart = xml.indexOf("<entry>");
                String latestEntry = xml.substring(entryStart);
                
                JsonObject video = new JsonObject();
                video.addProperty("videoId", extractValue(latestEntry, "<yt:videoId>(.*?)</yt:videoId>"));
                video.addProperty("title", extractValue(latestEntry, "<title>(.*?)</title>"));
                video.addProperty("thumbnail", "https://i.ytimg.com/vi/" + video.get("videoId").getAsString() + "/hqdefault.jpg");
                
                return Optional.of(video);
            }
        } catch (Exception e) {
            log.warn("YouTube RSS: Could not fetch videos for {}: {}", channelId, e.getMessage());
        }
        return Optional.empty();
    }

    private String resolveChannelId(String input) {
        // Step 1: Try oembed (Official metadata API)
        try {
            String handle = input.startsWith("@") ? input : "@" + input;
            String oembedUrl = "https://www.youtube.com/oembed?url=https://www.youtube.com/" + handle + "&format=json";
            String jsonResponse = restTemplate.getForObject(oembedUrl, String.class);
            if (jsonResponse != null) {
                JsonObject json = JsonParser.parseString(jsonResponse).getAsJsonObject();
                if (json.has("author_url")) {
                    String authorUrl = json.get("author_url").getAsString();
                    if (authorUrl.contains("/channel/")) {
                        return authorUrl.substring(authorUrl.lastIndexOf("/") + 1);
                    }
                }
            }
        } catch (Exception e) {
            // oembed failed, move to scraping
        }

        // Step 2: Scrape HTML for channelId
        try {
            String handle = input.startsWith("@") ? input : "@" + input;
            String url = "https://www.youtube.com/" + handle;
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            String html = response.getBody();
            if (html != null) {
                Pattern p = Pattern.compile("channelId\":\"(UC[a-zA-Z0-9_-]+)\"");
                Matcher m = p.matcher(html);
                if (m.find()) return m.group(1);
            }
        } catch (Exception e) {
            log.warn("YouTube Resolver (Scrape): Failed for {}: {}", input, e.getMessage());
        }
        
        return input;
    }

    private String extractValue(String xml, String regex) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(xml);
        if (matcher.find()) return matcher.group(1);
        return "";
    }
}
