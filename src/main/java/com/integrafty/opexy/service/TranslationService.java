package com.integrafty.opexy.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
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

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<byte[]> responseEntity = restTemplate.exchange(uri, HttpMethod.GET, entity, byte[].class);
            byte[] body = responseEntity.getBody();
            
            if (body == null) return "Error: No response from translation engine.";
            String response = new String(body, StandardCharsets.UTF_8);

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
