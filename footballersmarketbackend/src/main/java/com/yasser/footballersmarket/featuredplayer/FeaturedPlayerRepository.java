package com.yasser.footballersmarket.featuredplayer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FeaturedPlayerRepository  extends JpaRepository<FeaturedPlayer, Long> {
    List<FeaturedPlayer> findByDate(LocalDate date);
}
