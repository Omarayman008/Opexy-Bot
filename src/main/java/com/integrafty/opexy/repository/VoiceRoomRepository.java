package com.integrafty.opexy.repository;

import com.integrafty.opexy.entity.VoiceRoomEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface VoiceRoomRepository extends JpaRepository<VoiceRoomEntity, String> {
    Optional<VoiceRoomEntity> findByChannelId(String channelId);
    void deleteByChannelId(String channelId);
}
