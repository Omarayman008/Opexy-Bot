package com.integrafty.opexy.repository;

import com.integrafty.opexy.entity.NotificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {
    List<NotificationEntity> findAllByPlatformAndActiveTrue(String platform);
    List<NotificationEntity> findAllByGuildId(String guildId);
}
