package com.integrafty.opexy.repository;

import com.integrafty.opexy.entity.VoiceRoomEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

public interface VoiceRoomRepository extends JpaRepository<VoiceRoomEntity, String> {
    Optional<VoiceRoomEntity> findByChannelId(String channelId);
    List<VoiceRoomEntity> findAllByOwnerId(String ownerId);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM voice_rooms WHERE owner_id = :ownerId", nativeQuery = true)
    void deleteAllByOwnerIdNative(String ownerId);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM voice_rooms WHERE channel_id = :channelId", nativeQuery = true)
    void deleteByChannelIdNative(String channelId);
}
