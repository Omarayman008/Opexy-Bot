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
            String url = String.format("https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=%s&dt=t&q=%s",
                    targetLang, URLEncoder.encode(text, StandardCharsets.UTF_8));
            
            String response = restTemplate.getForObject(url, String.class);
            if (response == null) return "Error: No response from translation engine.";

            JSONArray jsonArray = new JSONArray(response);
            JSONArray sentences = jsonArray.getJSONArray(0);
            StringBuilder sb = new StringBuilder();

            for (int i = 0; i < sentences.length(); i++) {
                sb.append(sentences.getJSONArray(i).getString(0));
            }

            return sb.toString();
        } catch (Exception e) {
            log.error("Translation failed", e);
            return "Error: " + e.getMessage();
        }
    }
}
