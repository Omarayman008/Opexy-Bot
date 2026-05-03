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

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }
    public String getChannelId() { return channelId; }
    public void setChannelId(String channelId) { this.channelId = channelId; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getLastContentId() { return lastContentId; }
    public void setLastContentId(String lastContentId) { this.lastContentId = lastContentId; }
    public String getGuildId() { return guildId; }
    public void setGuildId(String guildId) { this.guildId = guildId; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
