package com.integrafty.opexy.service.notification;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
        try {
            String targetUrl = "https://kick.com/api/v1/channels/" + username;
            String encoded = URLEncoder.encode(targetUrl, StandardCharsets.UTF_8);
            String scraperUrl = "https://api.scraperapi.com/?api_key=" + SCRAPER_API_KEY + "&url=" + encoded + "&render=true";

            log.info("Checking Kick stream status for: {}", username);
            String body = restTemplate.getForObject(scraperUrl, String.class);
            if (body != null) {
                JsonObject json = JsonParser.parseString(body).getAsJsonObject();
                if (json.has("livestream") && !json.get("livestream").isJsonNull()) {
                    log.info("Kick: {} is LIVE", username);
                    return Optional.of(json.getAsJsonObject("livestream"));
                }
                log.info("Kick: {} is currently offline", username);
                return Optional.empty();
            } else {
                log.warn("Kick: Received empty body from ScraperAPI for {}", username);
            }
        } catch (Exception e) {
            log.error("Kick (ScraperAPI) Error for {}: {}", username, e.getMessage());
        }
        return Optional.empty();
    }
}

