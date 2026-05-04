package com.integrafty.opexy.service;

import com.integrafty.opexy.entity.UserEntity;
import com.integrafty.opexy.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EconomyService {

    private final UserRepository userRepository;

    public long getBalance(String userId, String guildId) {
        return userRepository.findByUserIdAndGuildId(userId, guildId)
                .map(UserEntity::getBalance)
                .orElse(0L);
    }

    @Transactional
    public void addBalance(String userId, String guildId, long amount) {
        UserEntity user = userRepository.findByUserIdAndGuildId(userId, guildId)
                .orElse(new UserEntity(userId, guildId, 0, 0, false, null, null, 0, 0));
        user.setBalance(user.getBalance() + amount);
        user.setTotalEarned(user.getTotalEarned() + (amount > 0 ? amount : 0));
        userRepository.save(user);
    }

    @Transactional
    public boolean subtractBalance(String userId, String guildId, long amount) {
        Optional<UserEntity> userOpt = userRepository.findByUserIdAndGuildId(userId, guildId);
        if (userOpt.isPresent()) {
            UserEntity user = userOpt.get();
            if (user.getBalance() >= amount) {
                user.setBalance(user.getBalance() - amount);
                userRepository.save(user);
                return true;
            }
        }
        return false;
    }
}
