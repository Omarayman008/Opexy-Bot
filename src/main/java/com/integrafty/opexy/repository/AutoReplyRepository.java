package com.integrafty.opexy.repository;

import com.integrafty.opexy.entity.AutoReplyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AutoReplyRepository extends JpaRepository<AutoReplyEntity, Long> {
    Optional<AutoReplyEntity> findByKeywordIgnoreCase(String keyword);
    void deleteByKeywordIgnoreCase(String keyword);
}
