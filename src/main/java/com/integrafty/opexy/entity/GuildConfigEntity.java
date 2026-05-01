package com.integrafty.opexy.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "guild_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GuildConfigEntity {

    @Id
    @Column(name = "guild_id", unique = true, nullable = false)
    private String guildId;

    @Column(name = "welcome_ch")
    private String welcomeChannelId;

    @Column(name = "verify_ch")
    private String verifyChannelId;

    @Column(name = "log_ch")
    private String logChannelId;

    @Column(name = "mute_role")
    private String muteRoleId;

    @Column(name = "verified_role")
    private String verifiedRoleId;
}
