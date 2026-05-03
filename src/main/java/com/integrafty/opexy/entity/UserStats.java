package com.integrafty.opexy.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_stats")
public class UserStats {

    public UserStats() {}
    public UserStats(Long userId) { this.userId = userId; }

    @Id
    @Column(name = "user_id")
    private Long userId;

    // Auction Achievements
    @Column(name = "failed_bids", columnDefinition = "int default 0")
    private int failedBids = 0;

    @Column(name = "success_bids", columnDefinition = "int default 0")
    private int successBids = 0;

    @Column(name = "max_bid", columnDefinition = "bigint default 0")
    private long maxBid = 0;

    // Mafia Achievements
    @Column(name = "mafia_wins", columnDefinition = "int default 0")
    private int mafiaWins = 0;

    @Column(name = "citizen_count", columnDefinition = "int default 0")
    private int citizenCount = 0;

    @Column(name = "votes_received", columnDefinition = "int default 0")
    private int votesReceived = 0;

    @Column(name = "detective_reveals", columnDefinition = "int default 0")
    private int detectiveReveals = 0;

    @Column(name = "doctor_saves", columnDefinition = "int default 0")
    private int doctorSaves = 0;

    // Minigame Achievements
    @Column(name = "pipe_wins", columnDefinition = "int default 0")
    private int pipeWins = 0;

    @Column(name = "speed_wins", columnDefinition = "int default 0")
    private int speedWins = 0;

    // Shop Items
    @Column(name = "shield_count", columnDefinition = "int default 0")
    private int shieldCount = 0;

    @Column(name = "double_reward", columnDefinition = "boolean default false")
    private boolean doubleRewardActive = false;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public int getFailedBids() { return failedBids; }
    public void setFailedBids(int failedBids) { this.failedBids = failedBids; }

    public int getSuccessBids() { return successBids; }
    public void setSuccessBids(int successBids) { this.successBids = successBids; }

    public long getMaxBid() { return maxBid; }
    public void setMaxBid(long maxBid) { this.maxBid = maxBid; }

    public int getMafiaWins() { return mafiaWins; }
    public void setMafiaWins(int mafiaWins) { this.mafiaWins = mafiaWins; }

    public int getCitizenCount() { return citizenCount; }
    public void setCitizenCount(int citizenCount) { this.citizenCount = citizenCount; }

    public int getVotesReceived() { return votesReceived; }
    public void setVotesReceived(int votesReceived) { this.votesReceived = votesReceived; }

    public int getDetectiveReveals() { return detectiveReveals; }
    public void setDetectiveReveals(int detectiveReveals) { this.detectiveReveals = detectiveReveals; }

    public int getDoctorSaves() { return doctorSaves; }
    public void setDoctorSaves(int doctorSaves) { this.doctorSaves = doctorSaves; }

    public int getPipeWins() { return pipeWins; }
    public void setPipeWins(int pipeWins) { this.pipeWins = pipeWins; }

    public int getSpeedWins() { return speedWins; }
    public void setSpeedWins(int speedWins) { this.speedWins = speedWins; }

    public int getShieldCount() { return shieldCount; }
    public void setShieldCount(int shieldCount) { this.shieldCount = shieldCount; }

    public boolean isDoubleRewardActive() { return doubleRewardActive; }
    public void setDoubleRewardActive(boolean doubleRewardActive) { this.doubleRewardActive = doubleRewardActive; }
}
