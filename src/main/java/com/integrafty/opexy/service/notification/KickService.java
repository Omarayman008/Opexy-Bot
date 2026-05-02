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

@Service
@RequiredArgsConstructor
@Slf4j
public class KickService {

    private final RestTemplate restTemplate = new RestTemplate();

    public Optional<JsonObject> getStreamStatus(String username) {
        try {
            // Priority 1: Use Kick API v1 with browser-like headers
            String url = "https://kick.com/api/v1/channels/" + username;
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            headers.set("Accept", "application/json");
            headers.set("Referer", "https://kick.com/");
            
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            String body = response.getBody();
            
            if (body != null) {
                JsonObject json = JsonParser.parseString(body).getAsJsonObject();
                if (json.has("livestream") && !json.get("livestream").isJsonNull()) {
                    return Optional.of(json.getAsJsonObject("livestream"));
                }
            }
        } catch (Exception e) {
            // API blocked, try fallbacks
        }
        
        // Priority 2: Use decapi.me proxy (Case sensitivity might matter for some proxies)
        return fetchFromDecapi(username);
    }

    private Optional<JsonObject> fetchFromDecapi(String username) {
        try {
            // Decapi is usually the most reliable for cloud environments
            String isLive = restTemplate.getForObject("https://decapi.me/kick/is_live/" + username, String.class);
            
            if (isLive != null && (isLive.trim().equalsIgnoreCase("true") || isLive.trim().equalsIgnoreCase("online"))) {
                JsonObject status = new JsonObject();
                status.addProperty("is_live", true);
                
                String title = restTemplate.getForObject("https://decapi.me/kick/title/" + username, String.class);
                status.addProperty("title", title != null ? title : "Live on Kick!");
                
                return Optional.of(status);
            }
        } catch (Exception e) {
            log.warn("Decapi Fallback: Failed for {}: {}", username, e.getMessage());
        }
        return Optional.empty();
    }
}
