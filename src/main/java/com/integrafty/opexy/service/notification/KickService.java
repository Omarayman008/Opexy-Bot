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
            String url = "https://kick.com/api/v1/channels/" + username;
            
            // Set User-Agent to avoid 403/Forbidden from Cloudflare/Kick
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36");
            headers.set("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8");
            headers.set("Accept-Language", "en-US,en;q=0.9");
            headers.set("Referer", "https://kick.com/" + username);
            headers.set("Cache-Control", "max-age=0");
            
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonObject json = JsonParser.parseString(response.getBody()).getAsJsonObject();
                return Optional.of(json);
            }
        } catch (Exception e) {
            log.warn("Kick API: Could not fetch status for {}: {}", username, e.getMessage());
        }
        return Optional.empty();
    }
}
