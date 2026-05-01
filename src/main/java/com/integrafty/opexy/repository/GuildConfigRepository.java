package com.integrafty.opexy.repository;

import com.integrafty.opexy.entity.GuildConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GuildConfigRepository extends JpaRepository<GuildConfigEntity, String> {
}
