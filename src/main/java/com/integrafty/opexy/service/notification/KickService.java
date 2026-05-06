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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
            // Target the main channel page instead of the API for better bypass compatibility
            String targetUrl = "https://kick.com/" + cleanUsername;
            
            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("cmd", "request.get");
            requestBody.addProperty("url", targetUrl);
            requestBody.addProperty("maxTimeout", 60000);

            log.info("Checking Kick HTML: {} via FlareSolverr", cleanUsername);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(requestBody.toString(), headers);

            ResponseEntity<String> response = restTemplate.postForEntity(flaresolverrUrl, entity, String.class);
            String body = response.getBody();

            if (body != null && response.getStatusCode().is2xxSuccessful()) {
                JsonObject json = JsonParser.parseString(body).getAsJsonObject();
                if (json.has("solution")) {
                    String html = json.getAsJsonObject("solution").get("response").getAsString();
                    
                    // Look for the livestream data in the HTML (it's often embedded in a script tag as JSON)
                    // We look for a pattern like "is_live":true or within the livestream object
                    if (html.contains("\"is_live\":true") || (html.contains("livestream") && html.contains("\"id\":"))) {
                        log.info("Kick: {} appears to be LIVE (HTML match)", cleanUsername);
                        
                        // Create a dummy livestream object for the scheduler
                        JsonObject dummyLive = new JsonObject();
                        dummyLive.addProperty("id", cleanUsername + "_live_" + System.currentTimeMillis() / 3600000); // Hourly unique ID
                        dummyLive.addProperty("session_title", "Live on KICK!");
                        
                        // Try to extract real title if possible
                        Pattern pTitle = Pattern.compile("\"session_title\":\"(.*?)\"");
                        Matcher mTitle = pTitle.matcher(html);
                        if (mTitle.find()) dummyLive.addProperty("session_title", mTitle.group(1));

                        return Optional.of(dummyLive);
                    }
                    log.info("Kick: {} is currently offline", cleanUsername);
                }
            }
        } catch (Exception e) {
            log.error("FlareSolverr Scraping Error for {}: {}", cleanUsername, e.getMessage());
        }
        return Optional.empty();
    }
}
