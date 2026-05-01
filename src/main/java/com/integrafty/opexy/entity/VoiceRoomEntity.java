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
    private String channelId;

    private String ownerId;

    private String status; // OPEN, LOCKED, HIDDEN
}
