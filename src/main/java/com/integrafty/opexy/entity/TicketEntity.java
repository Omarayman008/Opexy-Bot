package com.integrafty.opexy.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "tickets")
@Data
public class TicketEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "channel_id", nullable = false)
    private String channelId;

    @Column(name = "category", nullable = false)
    private String category; // Support, WhiteList, Hiring, Complaint

    @Column(name = "status", nullable = false)
    private String status = "OPEN"; // OPEN, CLOSED

    @Column(name = "ticket_number")
    private Integer ticketNumber;

    @Column(name = "staff_id")
    private String staffId;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
