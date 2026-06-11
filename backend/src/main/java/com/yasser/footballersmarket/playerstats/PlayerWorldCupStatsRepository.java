package com.yasser.footballersmarket.playerstats;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlayerWorldCupStatsRepository extends JpaRepository<PlayerWorldCupStats, Long> {
    List<PlayerWorldCupStats> findByTeamId(Long teamId);
}
