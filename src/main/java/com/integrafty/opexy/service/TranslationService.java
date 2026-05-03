package com.integrafty.opexy.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
@Slf4j
public class TranslationService {

    public String translate(String text, String targetLang) {
        try {
            // Normalize language codes (e.g., ENG -> en, AR -> ar)
            String langCode = targetLang.toLowerCase();
            if (langCode.equals("eng")) langCode = "en";
            if (langCode.equals("ara")) langCode = "ar";
            if (langCode.length() > 2) langCode = langCode.substring(0, 2);

            URI uri = UriComponentsBuilder.fromHttpUrl("https://translate.googleapis.com/translate_a/single")
                    .queryParam("client", "gtx")
                    .queryParam("sl", "auto")
                    .queryParam("tl", langCode)
                    .queryParam("dt", "t")
                    .queryParam("ie", "UTF-8")
                    .queryParam("oe", "UTF-8")
                    .queryParam("q", text)
                    .build()
                    .toUri();
            
            log.info("Translating to {}: {}", langCode, uri);

            URL url = uri.toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
            
            StringBuilder responseBuilder = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    responseBuilder.append(line);
                }
            }
            
            String response = responseBuilder.toString();
            if (response.isEmpty()) return "Error: No response from translation engine.";

            JSONArray jsonArray = new JSONArray(response);
            if (jsonArray.isNull(0)) return "Error: Unexpected response structure.";
            
            JSONArray sentences = jsonArray.getJSONArray(0);
            StringBuilder sb = new StringBuilder();

            for (int i = 0; i < sentences.length(); i++) {
                JSONArray sentence = sentences.optJSONArray(i);
                if (sentence != null && !sentence.isNull(0)) {
                    sb.append(sentence.getString(0));
                }
            }

            String translated = sb.toString();
            return translated.isEmpty() ? "Error: Empty translation result." : translated;
        } catch (Exception e) {
            log.error("Translation failed", e);
            return "Error: " + e.getMessage();
        }
    }
}
