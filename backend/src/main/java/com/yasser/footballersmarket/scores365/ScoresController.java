package com.yasser.footballersmarket.scores365;

import com.yasser.footballersmarket.player.PlayerService;
import com.yasser.footballersmarket.player.dto.PlayerDetailsResDto;
import com.yasser.footballersmarket.scores365.dto.FeaturedMatch;
import com.yasser.footballersmarket.scores365.dto.FeaturedPlayersResponse;
import com.yasser.footballersmarket.scores365.dto.PlayerLeagueStats;
import com.yasser.footballersmarket.scores365.dto.PlayerRecentMatch;
import com.yasser.footballersmarket.sofascore.dto.SearchPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/ss")
public class ScoresController {
    private final ScoresService scoresService;
    private final PlayerService playerService;

    private final Logger logger = LoggerFactory.getLogger(ScoresController.class);

    public ScoresController(ScoresService scoresService, PlayerService playerService) {
        this.scoresService = scoresService;
        this.playerService = playerService;
    }

    @GetMapping("/featured-match")
    public ResponseEntity getFeaturedEventMatch(){
        try{
            FeaturedMatch footballFeaturedMatch = scoresService.getFootballFeaturedMatch();
            return ResponseEntity.ok(footballFeaturedMatch);
        }catch (Exception e){
            logger.error("scores service featured match error {}", e.getMessage());
            return ResponseEntity.internalServerError().body("error occurred");
        }

    }

    @GetMapping("/trending-players")
    public ResponseEntity getTrendingPlayers(){
        try{
            FeaturedPlayersResponse trendingPlayersResponse = scoresService.getFeaturedPlayers();

            return ResponseEntity.ok(trendingPlayersResponse);
        }catch (Exception e){
            logger.error("scores trending players error {}", e.getMessage());
            return ResponseEntity.internalServerError().body("error occurred");
        }

    }
    @GetMapping("/players")
    public ResponseEntity getPlayerDetails(@RequestParam(name = "externalId") Integer externalId){
        try {
            PlayerDetailsResDto playerDetailsById = scoresService.getPlayerDetailsById(externalId);
            return ResponseEntity.ok(playerDetailsById);
        }catch (EntityNotFoundException e){
            return ResponseEntity.badRequest().body("player not found");
        }catch (Exception e){
            return ResponseEntity.internalServerError().body("error occurred");
        }

    }

    @GetMapping("/players/last-ratings")
    public ResponseEntity getPlayerLastRatingDetails(@RequestParam(name = "externalId") Integer externalId){
        try {
            List<PlayerRecentMatch> playerRecentMatches = scoresService.fetchPlayerLastRatings(externalId);
            return ResponseEntity.ok(playerRecentMatches);
        }catch (EntityNotFoundException e){
            return ResponseEntity.badRequest().body("player not found");
        }catch (Exception e){
            return ResponseEntity.internalServerError().body("error occurred");
        }

    }

    @GetMapping("/players/search")
    public ResponseEntity getPlayerDetails(@RequestParam(name = "playerName") String playerName){
        try{
            logger.info("searching player name {}", playerName);
            List<SearchPlayerEntity> searchPlayerEntities = scoresService.searchByPlayerName(playerName);
            return ResponseEntity.ok(searchPlayerEntities);
        }catch (Exception e){
            logger.error("scores search player error {}", e.getMessage());
            return ResponseEntity.internalServerError().body("error occurred");
        }

    }
}
