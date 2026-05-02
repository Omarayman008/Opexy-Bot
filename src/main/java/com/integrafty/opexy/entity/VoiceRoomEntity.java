package com.integrafty.opexy.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "voice_rooms")
@Getter
@Setter
public class VoiceRoomEntity {

    @Id
    private String ownerId;

    @Column(nullable = true)
    private String channelId;

    private String roomName;

    private Integer userLimit;

    private Integer bitrate;

    private String status; // OPEN, LOCKED, HIDDEN
}
