package com.integrafty.opexy.service;

import com.integrafty.opexy.entity.WordFilterEntity;
import com.integrafty.opexy.repository.WordFilterRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class WordFilterService {

    private final WordFilterRepository wordFilterRepository;

    // In-memory cache
    private final Set<String> forbiddenWords = new HashSet<>();

    @PostConstruct
    public void init() {
        reload();
    }

    // Reload every 5 minutes
    @Scheduled(fixedDelay = 300_000)
    public void reload() {
        try {
            List<WordFilterEntity> all = wordFilterRepository.findAll();
            synchronized (forbiddenWords) {
                forbiddenWords.clear();
                all.forEach(e -> forbiddenWords.add(e.getWord().toLowerCase()));
            }
            log.info("Word filter reloaded. Total words: {}", forbiddenWords.size());
        } catch (Exception e) {
            log.error("Failed to reload word filter: {}", e.getMessage());
        }
    }

    @Transactional
    public void addWord(String word) {
        if (wordFilterRepository.findByWordIgnoreCase(word).isEmpty()) {
            WordFilterEntity entity = new WordFilterEntity();
            entity.setWord(word.toLowerCase());
            wordFilterRepository.save(entity);
        }
        reload();
    }

    @Transactional
    public void removeWord(String word) {
        wordFilterRepository.deleteByWordIgnoreCase(word);
        reload();
    }

    public boolean isForbidden(String content) {
        return findForbiddenWord(content) != null;
    }

    public String findForbiddenWord(String content) {
        String lower = content.toLowerCase();
        synchronized (forbiddenWords) {
            for (String word : forbiddenWords) {
                if (lower.contains(word)) return word;
            }
        }
        return null;
    }

    public Set<String> getAllWords() {
        synchronized (forbiddenWords) {
            return new HashSet<>(forbiddenWords);
        }
    }
}
