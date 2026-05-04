package com.integrafty.opexy.repository;

import com.integrafty.opexy.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, String> {
    Optional<UserEntity> findByUserIdAndGuildId(String userId, String guildId);
    java.util.List<UserEntity> findTop10ByGuildIdOrderByEventPointsDesc(String guildId);
}
