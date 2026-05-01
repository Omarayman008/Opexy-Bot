package com.integrafty.opexy.repository;

import com.integrafty.opexy.entity.TicketMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TicketMessageRepository extends JpaRepository<TicketMessageEntity, Long> {
    List<TicketMessageEntity> findAllByTicketIdOrderByCreatedAtAsc(String ticketId);
}
