package com.integrafty.opexy.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
@Slf4j
public class TranslationService {

    public String translate(String text, String targetLang) {
        try {
            // Normalize language codes
            String langCode = targetLang.toLowerCase();
            if (langCode.equals("eng")) langCode = "en";
            if (langCode.equals("ara")) langCode = "ar";
            if (langCode.length() > 2) langCode = langCode.substring(0, 2);

            // Using MyMemory API as a more stable alternative to Google's unofficial endpoint
            String urlStr = String.format("https://api.mymemory.translated.net/get?q=%s&langpair=auto|%s",
                    URLEncoder.encode(text, StandardCharsets.UTF_8.toString()), langCode);
            
            log.info("Translating to {} via MyMemory: {}", langCode, urlStr);

            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            
            StringBuilder responseBuilder = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    responseBuilder.append(line);
                }
            }
            
            String response = responseBuilder.toString();
            if (response.isEmpty()) return "Error: No response from translation engine.";

            JSONObject json = new JSONObject(response);
            if (json.has("responseData")) {
                return json.getJSONObject("responseData").getString("translatedText");
            }

            return "Error: Could not parse translation response.";
        } catch (Exception e) {
            log.error("Translation failed", e);
            return "Error: " + e.getMessage();
        }
    }
}
