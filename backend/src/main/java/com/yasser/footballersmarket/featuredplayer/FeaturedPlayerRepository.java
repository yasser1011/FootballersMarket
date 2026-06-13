package com.yasser.footballersmarket.featuredplayer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FeaturedPlayerRepository  extends JpaRepository<FeaturedPlayer, Long> {
    List<FeaturedPlayer> findByDate(LocalDate date);

    // world cup featured: a round's current stored top-5 (running best-of-round), loaded to merge
    // the latest match's ratings into before rewriting the round
    List<FeaturedPlayer> findByFixtureNameOrderByPositionAsc(String fixtureName);

    // bulk delete executes immediately, so a round can be replaced (delete then re-insert) within one
    // transaction without the unique constraint tripping on hibernate's insert-before-delete flush order
    @Modifying
    void deleteByFixtureName(String fixtureName);

    // display rule: the 5 most recent WC featured rows ≈ the current round's running top-5
    List<FeaturedPlayer> findTop5ByWorldCupTrueOrderByDateDescPositionAsc();
}
