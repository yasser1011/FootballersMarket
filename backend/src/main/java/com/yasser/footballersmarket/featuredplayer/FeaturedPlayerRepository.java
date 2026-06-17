package com.yasser.footballersmarket.featuredplayer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
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

    // a true bulk DML delete (NOT a derived select-then-remove): executes immediately and clears the
    // persistence context, so re-inserting a round's top-5 in the same transaction doesn't trip the
    // unique constraint on hibernate's insert-before-delete flush order.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from FeaturedPlayer fp where fp.fixtureName = ?1")
    void deleteByFixtureName(String fixtureName);

    // display rule: the 5 most recent WC featured rows ≈ the current round's running top-5
    List<FeaturedPlayer> findTop5ByWorldCupTrueOrderByDateDescPositionAsc();
}
