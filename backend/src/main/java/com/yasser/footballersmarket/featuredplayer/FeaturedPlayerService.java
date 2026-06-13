package com.yasser.footballersmarket.featuredplayer;

import com.yasser.footballersmarket.player.Player;
import com.yasser.footballersmarket.player.PlayerRepository;
import com.yasser.footballersmarket.scores365.ScoresService;
import com.yasser.footballersmarket.scores365.dto.FeaturedPlayersResponse;
import com.yasser.footballersmarket.sofascore.SofascoreConfig;
import com.yasser.footballersmarket.sofascore.SofascoreService;
import com.yasser.footballersmarket.sofascore.dto.TrendingPlayer;
import com.yasser.footballersmarket.sofascore.dto.TrendingPlayersResponse;
import com.yasser.footballersmarket.worldcup.WorldCupTeam;
import com.yasser.footballersmarket.worldcup.WorldCupTeamRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class FeaturedPlayerService {


    private final FeaturedPlayerRepository featuredPlayerRepository;
    private final ScoresService scoresService;
    private final PlayerRepository playerRepository;
    private final WorldCupTeamRepository worldCupTeamRepository;
    private final Logger logger = LoggerFactory.getLogger(FeaturedPlayerService.class);

    public FeaturedPlayerService(FeaturedPlayerRepository featuredPlayerRepository, ScoresService scoresService,
                                 PlayerRepository playerRepository, WorldCupTeamRepository worldCupTeamRepository){
        this.featuredPlayerRepository = featuredPlayerRepository;
        this.scoresService = scoresService;
        this.playerRepository = playerRepository;
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

    public FeaturedPlayersResponse getFeaturedPlayers(){
        List<FeaturedPlayer> currDateFeaturedPlayers = featuredPlayerRepository.
                findByDate(LocalDate.now());

        if (!currDateFeaturedPlayers.isEmpty()){
            logger.info("featured players fetched from database");
            FeaturedPlayersResponse featuredPlayersResponse = new FeaturedPlayersResponse(currDateFeaturedPlayers);
            scoresService.addBaseUrlToTrendingPlayersResponse(featuredPlayersResponse);
            return featuredPlayersResponse;
        }
        logger.info("fetching featured players from external service");
//        FeaturedPlayersResponse trendingPlayers = sofascoreService.getTrendingPlayers();
        FeaturedPlayersResponse trendingPlayers = scoresService.getFeaturedPlayers();
        if (trendingPlayers == null) return new FeaturedPlayersResponse(new ArrayList<>());

        try {
            logger.info("fetched featured players from external service saving to database");
            featuredPlayerRepository.saveAll(trendingPlayers.getFeaturedPlayers());
            logger.info("featured players saved to database");
            scoresService.addBaseUrlToTrendingPlayersResponse(trendingPlayers);

            return trendingPlayers;
        } catch (DataIntegrityViolationException e) {
            // there are already data saved for this date
            List<FeaturedPlayer> featuredPlayers = featuredPlayerRepository.
                    findByDate(LocalDate.now());

            logger.info("featured players fetching again from db");
            FeaturedPlayersResponse featResponse = new FeaturedPlayersResponse(featuredPlayers);
            scoresService.addBaseUrlToTrendingPlayersResponse(featResponse);
            return featResponse;

        }

    }
}
