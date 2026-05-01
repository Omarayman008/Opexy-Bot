package com.integrafty.opexy.repository;

import com.integrafty.opexy.entity.TicketEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<TicketEntity, Long> {
    Optional<TicketEntity> findByChannelId(String channelId);
    boolean existsByUserIdAndStatus(String userId, String status);
}
