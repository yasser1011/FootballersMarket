package com.yasser.footballersmarket.playerstats;

import com.yasser.footballersmarket.fixture.Team;
import com.yasser.footballersmarket.fixture.TeamRepository;
import com.yasser.footballersmarket.player.Player;
import com.yasser.footballersmarket.player.PlayerRepository;
import com.yasser.footballersmarket.scores365.dto.PlayerRecentMatch;
import org.springframework.stereotype.Service;

import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

// a player's last 5 matches from our own fixture data, in the same shape (newest first) the
// 365scores last-matches block has, so the details page renders either one
@Service
public class PlayerRecentMatchService {
    static final int RECENT_MATCHES = 5;

    private final PlayerMatchRatingRepository matchRatingRepository;
    private final TeamRepository teamRepository;
    private final PlayerRepository playerRepository;

    public PlayerRecentMatchService(PlayerMatchRatingRepository matchRatingRepository,
                                    TeamRepository teamRepository,
                                    PlayerRepository playerRepository) {
        this.matchRatingRepository = matchRatingRepository;
        this.teamRepository = teamRepository;
        this.playerRepository = playerRepository;
    }

    // empty = use 365scores instead: the player is not fixture-maintained (sofascore, sofascore_buy,
    // flagged -- his matches are not in our fixtures), we do not have 5 of his matches yet, or an
    // opponent has no team row to name him by
    public Optional<List<PlayerRecentMatch>> fromFixtures(Long playerId, String updatedBy, Integer missedMatches) {
        if (playerId == null || Player.refreshesExternally(updatedBy, missedMatches)) return Optional.empty();

        List<PlayerMatchRating> rows = matchRatingRepository.findTop5ByPlayerIdOrderByFixtureDateDesc(playerId);
        if (rows.size() < RECENT_MATCHES) return Optional.empty();

        Map<Long, Team> opponents = teamRepository.findAllById(rows.stream()
                        .map(PlayerMatchRating::getOpponentTeamId)
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList())
                .stream()
                .collect(Collectors.toMap(Team::getId, Function.identity()));
        // the page shortens the opponent's name, so a match without one cannot be shown
        if (rows.stream().anyMatch(row -> opponentName(opponents, row) == null)) return Optional.empty();

        // one lookup per opponent, not per row (a player can face the same club twice in 5)
        Map<Long, String> badges = new HashMap<>();
        return Optional.of(rows.stream()
                .map(row -> {
                    PlayerRecentMatch match = new PlayerRecentMatch(opponentName(opponents, row), row.getRating(),
                            // the 365 path puts 365's team id here; ours is an api-football id, so
                            // it is left out rather than mixing the two id spaces (unused by the page)
                            null,
                            row.getFixtureDate() != null ? row.getFixtureDate().atZone(ZoneOffset.UTC).toLocalDate() : null);
                    // a club we maintain carries its teammates' 365 badge; any other club has none
                    match.setOpponentTeamPhotoUrl(badges.computeIfAbsent(row.getOpponentTeamId(),
                            playerRepository::findMostCommonClubPhotoUrlOfTeam));
                    return match;
                })
                .toList());
    }

    private static String opponentName(Map<Long, Team> opponents, PlayerMatchRating row) {
        Team team = row.getOpponentTeamId() != null ? opponents.get(row.getOpponentTeamId()) : null;
        String name = team != null ? team.getName() : null;
        return name != null && !name.isBlank() ? name : null;
    }
}
