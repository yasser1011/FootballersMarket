package com.yasser.footballersmarket.featuredplayer;

import com.yasser.footballersmarket.scores365.ScoresService;
import com.yasser.footballersmarket.scores365.dto.FeaturedPlayersResponse;
import com.yasser.footballersmarket.sofascore.SofascoreConfig;
import com.yasser.footballersmarket.sofascore.SofascoreService;
import com.yasser.footballersmarket.sofascore.dto.TrendingPlayer;
import com.yasser.footballersmarket.sofascore.dto.TrendingPlayersResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class FeaturedPlayerService {

    @Autowired
    private FeaturedPlayerRepository featuredPlayerRepository;
    @Autowired
    private SofascoreService sofascoreService;
    @Autowired
    private ScoresService scoresService;
    @Autowired
    private SofascoreConfig sofascoreConfig;
    Logger logger = LoggerFactory.getLogger(FeaturedPlayerService.class);

    public FeaturedPlayersResponse getFeaturedPlayers(){
        List<FeaturedPlayer> currDateFeaturedPlayers = featuredPlayerRepository.
                findByDate(LocalDate.now());

        if (currDateFeaturedPlayers.size() > 0){
            logger.info("featured players fetched from database");
            FeaturedPlayersResponse featuredPlayersResponse = new FeaturedPlayersResponse(currDateFeaturedPlayers);
//            sofascoreService.addBaseUrlToTrendingPlayersResponse(featuredPlayersResponse);
            scoresService.addBaseUrlToTrendingPlayersResponse(featuredPlayersResponse);
            return featuredPlayersResponse;
        }
        logger.info("fetching featured players from external service");
//        FeaturedPlayersResponse trendingPlayers = sofascoreService.getTrendingPlayers();
        FeaturedPlayersResponse trendingPlayers = scoresService.getFeaturedPlayers();
        if (trendingPlayers == null) return new FeaturedPlayersResponse(new ArrayList<>());

        logger.info("fetched featured players from external service saving to database");
        featuredPlayerRepository.saveAll(trendingPlayers.getFeaturedPlayers());
        logger.info("featured players saved to database");
//        sofascoreService.addBaseUrlToTrendingPlayersResponse(trendingPlayers);
        scoresService.addBaseUrlToTrendingPlayersResponse(trendingPlayers);

        return trendingPlayers;
    }
}
