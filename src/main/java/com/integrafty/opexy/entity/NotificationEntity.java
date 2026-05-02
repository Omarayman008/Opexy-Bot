package com.integrafty.opexy.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "notifications")
@Getter
@Setter
public class NotificationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String platform; // KICK, TWITCH, YOUTUBE

    @Column(nullable = false)
    private String channelId; // Platform-specific ID or username

    private String displayName;

    private String lastContentId; // Stream ID or Video ID

    private String guildId;

    private boolean active = true;
}
