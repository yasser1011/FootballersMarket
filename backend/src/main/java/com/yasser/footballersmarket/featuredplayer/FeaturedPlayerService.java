package com.yasser.footballersmarket.featuredplayer;

import com.yasser.footballersmarket.fixture.TeamRepository;
import com.yasser.footballersmarket.fixture.tournament.TournamentCatalog;
import com.yasser.footballersmarket.player.Player;
import com.yasser.footballersmarket.player.PlayerRepository;
import com.yasser.footballersmarket.playerstats.PlayerMatchRatingRepository;
import com.yasser.footballersmarket.scores365.dto.FeaturedPlayersResponse;
import com.yasser.footballersmarket.worldcup.WorldCupTeamRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class FeaturedPlayerService {


    private final FeaturedPlayerRepository featuredPlayerRepository;
    private final PlayerMatchRatingRepository playerMatchRatingRepository;
    private final PlayerRepository playerRepository;
    private final TeamRepository teamRepository;
    private final WorldCupTeamRepository worldCupTeamRepository;

    public FeaturedPlayerService(FeaturedPlayerRepository featuredPlayerRepository,
                                 PlayerMatchRatingRepository playerMatchRatingRepository,
                                 PlayerRepository playerRepository, TeamRepository teamRepository,
                                 WorldCupTeamRepository worldCupTeamRepository){
        this.featuredPlayerRepository = featuredPlayerRepository;
        this.playerMatchRatingRepository = playerMatchRatingRepository;
        this.playerRepository = playerRepository;
        this.teamRepository = teamRepository;
        this.worldCupTeamRepository = worldCupTeamRepository;
    }

    // world cup featured players = the round's running top-5 (5 most recent WC rows). images come
    // from our db (player.photoUrl + world_cup_team.logoUrl), NOT 365 ids — WC players may have none.
    public FeaturedPlayersResponse getWorldCupFeaturedPlayers(){
        List<FeaturedPlayer> rows = featuredPlayerRepository.findTop5ByWorldCupTrueOrderByDateDescPositionAsc();
        if (rows.isEmpty()) return new FeaturedPlayersResponse(new ArrayList<>());

        Set<Long> playerIds = rows.stream().map(r -> r.getPlayerId().longValue()).collect(Collectors.toSet());
        Map<Long, String> photoByPlayerId = new HashMap<>();
        playerRepository.findAllById(playerIds).forEach(p -> photoByPlayerId.put(p.getId(), p.getPhotoUrl()));

        Set<Long> teamIds = rows.stream().filter(r -> r.getPlayerTeamId() != null)
                .map(r -> r.getPlayerTeamId().longValue()).collect(Collectors.toSet());
        Map<Long, String> logoByTeamId = new HashMap<>();
        worldCupTeamRepository.findAllById(teamIds).forEach(t -> logoByTeamId.put(t.getId(), t.getLogoUrl()));

        rows.forEach(r -> {
            r.setPlayerImgUrl(photoByPlayerId.get(r.getPlayerId().longValue()));
            if (r.getPlayerTeamId() != null) r.setPlayerTeamImgUrl(logoByTeamId.get(r.getPlayerTeamId().longValue()));
        });
        return new FeaturedPlayersResponse(rows);
    }

    // the club featured players: the 5 best match ratings of the leagues' current rounds, or of the
    // CL's alone while it is the newest (see PlayerMatchRatingRepository.findTop5OfCurrentRounds), straight from our own fixture
    // data -- no external call and nothing cached. the rows are built, never saved, in the shape the
    // card always got. nothing applied yet -> an empty list (the card says so)
    public FeaturedPlayersResponse getFeaturedPlayers(){
        List<Object[]> top = playerMatchRatingRepository.findTop5OfCurrentRounds(TournamentCatalog.CHAMPIONS_LEAGUE_ID);
        if (top.isEmpty()) return new FeaturedPlayersResponse(new ArrayList<>());

        Map<Long, Player> players = new HashMap<>();
        playerRepository.findAllById(top.stream().map(row -> idOf(row[0])).collect(Collectors.toSet()))
                .forEach(player -> players.put(player.getId(), player));
        Map<Long, String> teamNames = new HashMap<>();
        teamRepository.findAllById(top.stream().flatMap(row -> Stream.of(idOf(row[3]), idOf(row[4])))
                        .filter(Objects::nonNull).collect(Collectors.toSet()))
                .forEach(team -> teamNames.put(team.getId(), team.getName()));

        List<FeaturedPlayer> featured = new ArrayList<>();
        for (Object[] row : top) {
            Player player = players.get(idOf(row[0]));
            if (player == null) continue; // deleted since: nothing to show for him
            FeaturedPlayer card = new FeaturedPlayer(player.getName(), player.getSofascoreId(),
                    ratingText(((Number) row[1]).doubleValue()), featured.size() + 1, null,
                    teamNames.get(idOf(row[3])), teamNames.get(idOf(row[4])));
            // the same images his details page shows; the badge is his current club's
            card.setPlayerImgUrl(player.getPhotoUrl());
            card.setPlayerTeamImgUrl(player.getClubPhotoUrl());
            featured.add(card);
        }
        return new FeaturedPlayersResponse(featured);
    }

    private static Long idOf(Object column) {
        return column == null ? null : ((Number) column).longValue();
    }

    // one decimal, the way ratings are shown everywhere ("7.5", not "7.53")
    private static String ratingText(double rating) {
        return String.format(Locale.ROOT, "%.1f", rating);
    }
}
