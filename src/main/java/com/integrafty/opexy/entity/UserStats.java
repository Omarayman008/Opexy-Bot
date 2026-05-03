package com.integrafty.opexy.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_stats")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserStats {

    @Id
    @Column(name = "user_id")
    private Long userId;

    // Auction Achievements
    @Column(name = "failed_bids", defaultValue = "0")
    private int failedBids;

    @Column(name = "success_bids", defaultValue = "0")
    private int successBids;

    @Column(name = "max_bid", defaultValue = "0")
    private long maxBid;

    // Mafia Achievements
    @Column(name = "mafia_wins", defaultValue = "0")
    private int mafiaWins;

    @Column(name = "citizen_count", defaultValue = "0")
    private int citizenCount;

    @Column(name = "votes_received", defaultValue = "0")
    private int votesReceived;

    @Column(name = "detective_reveals", defaultValue = "0")
    private int detectiveReveals;

    @Column(name = "doctor_saves", defaultValue = "0")
    private int doctorSaves;

    // Minigame Achievements
    @Column(name = "pipe_wins", defaultValue = "0")
    private int pipeWins;

    @Column(name = "speed_wins", defaultValue = "0")
    private int speedWins;
}
