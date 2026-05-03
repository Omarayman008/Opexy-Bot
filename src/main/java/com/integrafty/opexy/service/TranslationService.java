package com.integrafty.opexy.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.json.JSONArray;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
@Slf4j
public class TranslationService {

    private final RestTemplate restTemplate;

    public String translate(String text, String targetLang) {
        try {
            // Normalize language codes (e.g., ENG -> en, AR -> ar)
            String langCode = targetLang.toLowerCase();
            if (langCode.equals("eng")) langCode = "en";
            if (langCode.equals("ara")) langCode = "ar";
            if (langCode.length() > 2) langCode = langCode.substring(0, 2);

            String url = String.format("https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=%s&dt=t&q=%s",
                    langCode, URLEncoder.encode(text, StandardCharsets.UTF_8));
            
            log.info("Translating to {}: {}", langCode, url);

            String response = restTemplate.getForObject(url, String.class);
            if (response == null) return "Error: No response from translation engine.";

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
