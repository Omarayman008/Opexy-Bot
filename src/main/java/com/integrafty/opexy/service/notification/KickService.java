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
                    
                    // Bulletproof Detection Logic
                    String lowerHtml = html.toLowerCase();
                    String lowerUser = cleanUsername.toLowerCase();
                    boolean isLive = false;

                    // 1. Meta Tag Check (Highly reliable if present)
                    if (lowerHtml.contains("watching " + lowerUser + " live")) {
                        isLive = true;
                    }

                    // 2. Playback URL Check
                    if (!isLive && lowerHtml.contains("playback_url") && lowerHtml.contains("/" + lowerUser + "/master.m3u8")) {
                        isLive = true;
                    }

                    // 3. Targeted JSON State Check
                    // Find the user's object and ensure we don't accidentally read a sidebar channel's object
                    if (!isLive) {
                        String[] searchKeys = {"\"slug\":\"" + lowerUser + "\"", "\"username\":\"" + lowerUser + "\""};
                        for (String searchKey : searchKeys) {
                            int searchIndex = lowerHtml.indexOf(searchKey);
                            while (searchIndex != -1 && !isLive) {
                                int livestreamIndex = lowerHtml.indexOf("\"livestream\":{", searchIndex);
                                
                                if (livestreamIndex != -1 && (livestreamIndex - searchIndex) < 2000) {
                                    String inBetween = lowerHtml.substring(searchIndex + searchKey.length(), livestreamIndex);
                                    // Ensure we haven't crossed into another user's object
                                    if (!inBetween.contains("\"slug\":") && !inBetween.contains("\"username\":")) {
                                        int isLiveIndex = lowerHtml.indexOf("\"is_live\":true", livestreamIndex);
                                        if (isLiveIndex != -1 && (isLiveIndex - livestreamIndex) < 500) {
                                            isLive = true;
                                        }
                                    }
                                }
                                searchIndex = lowerHtml.indexOf(searchKey, searchIndex + 1);
                            }
                        }
                    }

                    if (isLive) {
                        log.info("Kick: {} appears to be LIVE (HTML match)", cleanUsername);
                        
                        // Create a dummy livestream object for the scheduler
                        JsonObject dummyLive = new JsonObject();
                        dummyLive.addProperty("session_title", "Live on KICK!");
                        
                        // Extract real Stream ID from HTML (e.g., "livestream":{"id":123456)
                        Pattern pId = Pattern.compile("\"livestream\"\\s*:\\s*\\{\\s*\"id\"\\s*:\\s*(\\d+)");
                        Matcher mId = pId.matcher(html);
                        if (mId.find()) {
                            dummyLive.addProperty("id", mId.group(1));
                        } else {
                            // Fallback to hourly ID if real ID not found
                            dummyLive.addProperty("id", cleanUsername + "_live_" + System.currentTimeMillis() / 3600000);
                        }
                        
                        // Try to extract real title if possible
                        Pattern pTitle = Pattern.compile("\"session_title\"\\s*:\\s*\"(.*?)\"");
                        Matcher mTitle = pTitle.matcher(html);
                        if (mTitle.find()) dummyLive.addProperty("session_title", mTitle.group(1));

                        return Optional.of(dummyLive);
                    }
                    log.info("Kick: {} is currently offline", cleanUsername);
                } else {
                    log.warn("FlareSolverr response missing 'solution' for {}", cleanUsername);
                }
            } else {
                log.warn("FlareSolverr failed with status {}: {}", response.getStatusCode(), body);
            }
        } catch (Exception e) {
            log.error("FlareSolverr Scraping Error for {}: {}", cleanUsername, e.getMessage());
        }
        return Optional.empty();
    }
}
