package com.integrafty.opexy.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Instant;

@Service
@Slf4j
public class WhitelistSyncService {

    @Value("${supabase.url:jdbc:postgresql://aws-1-ap-south-1.pooler.supabase.com:6543/postgres?sslmode=require}")
    private String dbUrl;

    @Value("${supabase.user:postgres.ungifmcwoxnpeduzxxbr}")
    private String dbUser;

    @Value("${SUPABASE_PASSWORD:[YOUR-PASSWORD]}")
    private String dbPassword;

    public void syncToSupabase(String discord, String mc, String version, String type) {
        if (dbPassword.equals("[YOUR-PASSWORD]")) {
            log.warn("Supabase password not set. Skipping sync.");
            return;
        }

        String sql = "INSERT INTO whitelist (discord, mc, version, type, team, tag, created_at, modified_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, discord);
            pstmt.setString(2, mc);
            pstmt.setString(3, version);
            pstmt.setString(4, type);
            pstmt.setString(5, "EMPTY");
            pstmt.setString(6, "مقبول");
            pstmt.setTimestamp(7, Timestamp.from(Instant.now()));
            pstmt.setTimestamp(8, Timestamp.from(Instant.now()));

            pstmt.executeUpdate();
            log.info("Successfully synced whitelist entry to Supabase for user: {}", mc);

        } catch (Exception e) {
            log.error("Failed to sync to Supabase: {}", e.getMessage());
        }
    }
}
