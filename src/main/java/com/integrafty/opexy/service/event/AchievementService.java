package com.integrafty.opexy.service.event;

import com.integrafty.opexy.entity.UserStats;
import com.integrafty.opexy.repository.UserStatsRepository;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
public class AchievementService {

    private final UserStatsRepository statsRepository;

    @Value("${opexy.roles.achievements.jinxed-bidder}")
    private String jinxedBidderId;

    @Value("${opexy.roles.achievements.golden-gavel}")
    private String goldenGavelId;

    @Value("${opexy.roles.achievements.high-roller}")
    private String highRollerId;

    @Value("${opexy.roles.achievements.contract-killer}")
    private String contractKillerId;

    @Value("${opexy.roles.achievements.eternal-civilian}")
    private String eternalCivilianId;

    @Value("${opexy.roles.achievements.public-enemy}")
    private String publicEnemyId;

    @Value("${opexy.roles.achievements.eagle-eye}")
    private String eagleEyeId;

    @Value("${opexy.roles.achievements.guardian-angel}")
    private String guardianAngelId;

    @Value("${opexy.roles.achievements.master-plumber}")
    private String masterPlumberId;

    @Value("${opexy.roles.achievements.sonic-fingers}")
    private String sonicFingersId;

    public UserStats getStats(long userId) {
        return statsRepository.findByUserId(userId)
                .orElseGet(() -> statsRepository.save(new UserStats(userId)));
    }

    @Transactional
    public void updateStats(long userId, Guild guild, Consumer<UserStats> updateAction) {
        UserStats stats = getStats(userId);
        updateAction.accept(stats);
        statsRepository.save(stats);
        checkAchievements(userId, guild, stats);
    }

    private void checkAchievements(long userId, Guild guild, UserStats stats) {
        Member member = guild.getMemberById(userId);
        if (member == null) return;

        // Auction Achievements
        checkAndGrant(member, jinxedBidderId, stats.getFailedBids() >= 5);
        checkAndGrant(member, goldenGavelId, stats.getSuccessBids() >= 5);
        checkAndGrant(member, highRollerId, stats.getMaxBid() >= 100000);

        // Mafia Achievements
        checkAndGrant(member, contractKillerId, stats.getMafiaWins() >= 6);
        checkAndGrant(member, eternalCivilianId, stats.getCitizenCount() >= 8);
        checkAndGrant(member, publicEnemyId, stats.getVotesReceived() >= 15);
        checkAndGrant(member, eagleEyeId, stats.getDetectiveReveals() >= 1);
        checkAndGrant(member, guardianAngelId, stats.getDoctorSaves() >= 3);

        // Minigame Achievements
        checkAndGrant(member, masterPlumberId, stats.getPipeWins() >= 4);
        checkAndGrant(member, sonicFingersId, stats.getSpeedWins() >= 1);
    }

    private void checkAndGrant(Member member, String roleId, boolean condition) {
        if (condition && roleId != null && !roleId.isEmpty()) {
            Role role = member.getGuild().getRoleById(roleId);
            if (role != null && !member.getRoles().contains(role)) {
                member.getGuild().addRoleToMember(member, role).queue();
            }
        }
    }
}
