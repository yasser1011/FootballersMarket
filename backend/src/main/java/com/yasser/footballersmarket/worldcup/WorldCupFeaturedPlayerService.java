package com.yasser.footballersmarket.worldcup;

import com.yasser.footballersmarket.featuredplayer.FeaturedPlayer;
import com.yasser.footballersmarket.featuredplayer.FeaturedPlayerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// maintains the per-ROUND running top-5 featured players. as each match of a round finishes its
// rated players are merged into the round's stored 5 (best-of-round); a player keeps/upgrades to
// his better rating. the round's 5 rows are replaced atomically (delete then insert) so membership
// and positions can shift freely from one match.
@Service
public class WorldCupFeaturedPlayerService {
    private final FeaturedPlayerRepository featuredPlayerRepository;
    private final Logger logger = LoggerFactory.getLogger(WorldCupFeaturedPlayerService.class);

    public WorldCupFeaturedPlayerService(FeaturedPlayerRepository featuredPlayerRepository) {
        this.featuredPlayerRepository = featuredPlayerRepository;
    }

    // a tracked WC player's rating in a single match (rating is the api string, e.g. "7.2")
    public record RatedWcPlayer(Long playerId, String name, String rating, Long teamId) {}

    @Transactional
    public void updateRoundTopFive(String round, LocalDate matchDate, List<RatedWcPlayer> ratedPlayers) {
        if (round == null || round.isBlank() || ratedPlayers.isEmpty()) return;

        // start from the round's current stored rows, keyed by player so a returning player merges
        Map<Long, Candidate> best = new LinkedHashMap<>();
        for (FeaturedPlayer row : featuredPlayerRepository.findByFixtureNameOrderByPositionAsc(round)) {
            best.put(row.getPlayerId().longValue(), new Candidate(row.getPlayerId().longValue(),
                    row.getPlayerName(), row.getRating(), parseRating(row.getRating()),
                    row.getPlayerTeamId() == null ? null : row.getPlayerTeamId().longValue(), row.getDate()));
        }

        // merge this match: a player enters, or upgrades only if this match's rating is better
        for (RatedWcPlayer rp : ratedPlayers) {
            double rating = parseRating(rp.rating());
            Candidate current = best.get(rp.playerId());
            if (current == null || rating > current.ratingValue()) {
                best.put(rp.playerId(), new Candidate(rp.playerId(), rp.name(), rp.rating(), rating,
                        rp.teamId(), matchDate));
            }
        }

        // top 5 by rating desc, ties broken by the earlier date (stable, so the longer-standing row wins)
        List<Candidate> top = new ArrayList<>(best.values());
        top.sort(Comparator.comparingDouble(Candidate::ratingValue).reversed()
                .thenComparing(Candidate::date));
        if (top.size() > 5) top = top.subList(0, 5);

        // replace the round atomically: bulk delete runs now, so the inserts below don't collide
        featuredPlayerRepository.deleteByFixtureName(round);
        List<FeaturedPlayer> rows = new ArrayList<>();
        int position = 1;
        for (Candidate c : top) {
            rows.add(FeaturedPlayer.worldCupRow(c.name(), c.playerId().intValue(), c.rating(),
                    c.teamId() == null ? null : c.teamId().intValue(), c.date(), position++, round));
        }
        featuredPlayerRepository.saveAll(rows);
        logger.info("world cup featured: round '{}' top-5 rebuilt ({} rows)", round, rows.size());
    }

    private double parseRating(String rating) {
        try {
            return rating == null ? 0.0 : Double.parseDouble(rating);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private record Candidate(Long playerId, String name, String rating, double ratingValue,
                             Long teamId, LocalDate date) {}
}
