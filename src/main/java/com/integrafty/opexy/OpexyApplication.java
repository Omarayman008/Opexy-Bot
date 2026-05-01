package com.integrafty.opexy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.annotation.Value;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

import lombok.extern.slf4j.Slf4j;
import javax.security.auth.login.LoginException;

@SpringBootApplication
@Slf4j
public class OpexyApplication {

    public static void main(String[] args) {
        SpringApplication.run(OpexyApplication.class, args);
    }

    @Bean
    public JDA jda(@Value("${discord.bot.token}") String token) {
        log.info("Starting HighCore Mc...");
        try {
            return JDABuilder.createDefault(token)
                .enableIntents(
                    GatewayIntent.GUILD_MEMBERS,
                    GatewayIntent.GUILD_MESSAGES,
                    GatewayIntent.MESSAGE_CONTENT,
                    GatewayIntent.GUILD_VOICE_STATES
                )
                .setMemberCachePolicy(MemberCachePolicy.ALL)
                .enableCache(CacheFlag.VOICE_STATE)
                .build();
        } catch (Exception e) {
            log.error("Failed to start Discord Bot", e);
            throw new RuntimeException("Failed to connect to Discord", e);
        }
    }
}
