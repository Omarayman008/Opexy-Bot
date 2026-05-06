package com.integrafty.opexy.service.notification;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class KickService {

    private final RestTemplate restTemplate = new RestTemplate();

    private static final String SCRAPER_API_KEY = "a0274a8e4d408eecd968e0544bc4a20f";

    public Optional<JsonObject> getStreamStatus(String username) {
        if (username == null || username.isBlank())
            return Optional.empty();

        String cleanUsername = username.trim();
        try {
            // Construct the target Kick API URL
            String targetUrl = "https://kick.com/api/v1/channels/" + cleanUsername;
            
            // Use UriComponentsBuilder for robust URL construction and encoding
            String scraperUrl = org.springframework.web.util.UriComponentsBuilder
                    .fromHttpUrl("https://api.scraperapi.com/")
                    .queryParam("api_key", SCRAPER_API_KEY)
                    .queryParam("url", targetUrl)
                    .build()
                    .toUriString();

            log.info("Checking Kick: {} | Target: {}", cleanUsername, targetUrl);
            
            ResponseEntity<String> response = restTemplate.getForEntity(scraperUrl, String.class);
            String body = response.getBody();

            if (body != null && response.getStatusCode().is2xxSuccessful()) {
                JsonObject json = JsonParser.parseString(body).getAsJsonObject();
                if (json.has("livestream") && !json.get("livestream").isJsonNull()) {
                    log.info("Kick: {} is LIVE", cleanUsername);
                    return Optional.of(json.getAsJsonObject("livestream"));
                }
                log.info("Kick: {} is currently offline", cleanUsername);
            } else {
                log.warn("Kick: Failed to get valid response for {}. Status: {}", cleanUsername,
                        response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Kick (ScraperAPI) Error for {}: {}", cleanUsername, e.getMessage());
        }
        return Optional.empty();
    }
}
