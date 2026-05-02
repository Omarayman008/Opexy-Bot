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
            // Priority 1: Use Kick API v1 with browser headers
            String url = "https://kick.com/api/v1/channels/" + username;
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36");
            headers.set("Accept", "application/json");
            headers.set("Referer", "https://kick.com/" + username);
            
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
            // Priority 2: Try scraping HTML if API is blocked
            Optional<JsonObject> scraped = scrapeHtml(username);
            if (scraped.isPresent()) return scraped;
        }
        
        // Priority 3: Final fallback using decapi.me proxy (Most reliable for cloud IPs)
        return fetchFromDecapi(username);
    }

    private Optional<JsonObject> scrapeHtml(String username) {
        try {
            String url = "https://kick.com/" + username;
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36");
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            String html = response.getBody();
            
            if (html != null && html.contains("\"is_live\":true")) {
                JsonObject status = new JsonObject();
                status.addProperty("is_live", true);
                if (html.contains("\"session_title\":\"")) {
                    String title = html.substring(html.indexOf("\"session_title\":\"") + 17);
                    title = title.substring(0, title.indexOf("\""));
                    status.addProperty("title", title);
                }
                return Optional.of(status);
            }
        } catch (Exception ex) {
            log.warn("Kick Scraper: Could not fetch status for {}: {}", username, ex.getMessage());
        }
        return Optional.empty();
    }

    private Optional<JsonObject> fetchFromDecapi(String username) {
        try {
            String isLive = restTemplate.getForObject("https://decapi.me/kick/is_live/" + username, String.class);
            if (isLive != null && isLive.trim().equalsIgnoreCase("true")) {
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
