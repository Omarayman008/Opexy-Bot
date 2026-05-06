package com.integrafty.opexy.service.notification;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
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

    private static final String DEFAULT_FLARESOLVERR_URL = "http://localhost:8191/v1";

    public Optional<JsonObject> getStreamStatus(String username) {
        if (username == null || username.isBlank()) return Optional.empty();
        
        String cleanUsername = username.trim();
        String flaresolverrUrl = System.getenv().getOrDefault("FLARESOLVERR_URL", DEFAULT_FLARESOLVERR_URL);
        
        try {
            String targetUrl = "https://kick.com/api/v1/channels/" + cleanUsername;
            
            // Construct FlareSolverr request body
            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("cmd", "request.get");
            requestBody.addProperty("url", targetUrl);
            requestBody.addProperty("maxTimeout", 60000);

            log.info("Checking Kick: {} via FlareSolverr at {}", cleanUsername, flaresolverrUrl);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(requestBody.toString(), headers);

            ResponseEntity<String> response = restTemplate.postForEntity(flaresolverrUrl, entity, String.class);
            String body = response.getBody();

            if (body != null && response.getStatusCode().is2xxSuccessful()) {
                JsonObject json = JsonParser.parseString(body).getAsJsonObject();
                if (json.has("solution")) {
                    JsonObject solution = json.getAsJsonObject("solution");
                    String responseString = solution.get("response").getAsString();
                    
                    // The response from FlareSolverr for a JSON API is the JSON string itself
                    JsonObject kickData = JsonParser.parseString(responseString).getAsJsonObject();
                    
                    if (kickData.has("livestream") && !kickData.get("livestream").isJsonNull()) {
                        log.info("Kick: {} is LIVE", cleanUsername);
                        return Optional.of(kickData.getAsJsonObject("livestream"));
                    }
                    log.info("Kick: {} is currently offline", cleanUsername);
                }
            } else {
                log.warn("FlareSolverr: Non-OK response for {}. Status: {}", cleanUsername, response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("FlareSolverr Error for {}: {}", cleanUsername, e.getMessage());
        }
        return Optional.empty();
    }
}
