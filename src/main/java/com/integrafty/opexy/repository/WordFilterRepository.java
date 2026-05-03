package com.integrafty.opexy.repository;

import com.integrafty.opexy.entity.WordFilterEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WordFilterRepository extends JpaRepository<WordFilterEntity, Long> {
    Optional<WordFilterEntity> findByWordIgnoreCase(String word);
    void deleteByWordIgnoreCase(String word);
}
