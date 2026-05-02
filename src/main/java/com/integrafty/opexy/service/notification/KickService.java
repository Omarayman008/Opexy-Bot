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

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36");
        headers.set("Accept", "application/json");
        headers.set("Accept-Language", "en-US,en;q=0.9");
        headers.set("Referer", "https://kick.com/");
        headers.set("Origin", "https://kick.com");
        return headers;
    }

    public Optional<JsonObject> getStreamStatus(String username) {
        // Priority 1: Kick API v2
        try {
            String url = "https://kick.com/api/v2/channels/" + username;
            HttpEntity<String> entity = new HttpEntity<>(buildHeaders());
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            String body = response.getBody();
            if (body != null) {
                JsonObject json = JsonParser.parseString(body).getAsJsonObject();
                if (json.has("livestream") && !json.get("livestream").isJsonNull()) {
                    return Optional.of(json.getAsJsonObject("livestream"));
                }
                return Optional.empty();
            }
        } catch (Exception e) {
            log.debug("Kick API v2: Failed for {}: {}", username, e.getMessage());
        }

        // Priority 2: Kick API v1
        try {
            String url = "https://kick.com/api/v1/channels/" + username;
            HttpEntity<String> entity = new HttpEntity<>(buildHeaders());
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            String body = response.getBody();
            if (body != null) {
                JsonObject json = JsonParser.parseString(body).getAsJsonObject();
                if (json.has("livestream") && !json.get("livestream").isJsonNull()) {
                    return Optional.of(json.getAsJsonObject("livestream"));
                }
                return Optional.empty();
            }
        } catch (Exception e) {
            log.debug("Kick API v1: Failed for {}: {}", username, e.getMessage());
        }

        return Optional.empty();
    }
}
