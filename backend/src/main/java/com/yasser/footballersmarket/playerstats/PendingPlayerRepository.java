package com.yasser.footballersmarket.playerstats;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PendingPlayerRepository extends JpaRepository<PendingPlayer, Long> {
}
