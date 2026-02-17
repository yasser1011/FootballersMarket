package com.yasser.footballersmarket.scores365;

import com.yasser.footballersmarket.player.PlayerService;
import com.yasser.footballersmarket.player.dto.PlayerDetailsResDto;
import com.yasser.footballersmarket.scores365.dto.FeaturedMatch;
import com.yasser.footballersmarket.scores365.dto.FeaturedPlayersResponse;
import com.yasser.footballersmarket.scores365.dto.PlayerLeagueStats;
import com.yasser.footballersmarket.sofascore.dto.SearchPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/ss")
public class ScoresController {
    private final ScoresService scoresService;
    private final PlayerService playerService;

    Logger logger = LoggerFactory.getLogger(ScoresController.class);

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
    public ResponseEntity getPlayerDetails(@RequestParam(name = "sofascoreId") Integer sofascoreId,
                                           @RequestParam(name = "rapidId", required = false) Long rapidId){
        try{
            if(rapidId == null){
                // maybe user bought a player that wasn't in db and then refreshed on old sf/{external_id} url he will not see the sell button
                PlayerDetailsResDto playerByExternalServiceId = playerService.getPlayerBySofascoreId(sofascoreId);
                if(playerByExternalServiceId != null){
                    Long playerInternalId = playerByExternalServiceId.getId();
                    logger.info("found in database from external service id." +
                                    " getting player details from scores, player id {} scores id {}",
                            playerInternalId, sofascoreId);
                    PlayerDetailsResDto playerDetailsById = playerService.getPlayerDetailsById(
                            playerInternalId, sofascoreId);
                    return ResponseEntity.ok(playerDetailsById);
                }else{
                    // not found in database
                    logger.info("not found in database from external service id, " +
                            "getting player details from scores, scores id {}", sofascoreId);

                    PlayerDetailsResDto playerEntityFromExternalService = scoresService.getPlayerEntityFromExternalService(sofascoreId);
                    if(playerEntityFromExternalService.getLeagueStats() == null){
                        com.yasser.footballersmarket.playerstats.PlayerLeagueStats playerEntityLeagueStats =
                                new com.yasser.footballersmarket.playerstats.PlayerLeagueStats(0,
                                0, 0, 6.5);
                        playerEntityFromExternalService.setLeagueStats(playerEntityLeagueStats);
                    }
                    return ResponseEntity.ok(playerEntityFromExternalService);
                }
            }else{
                logger.info("getting player details from scores, player id {} scores id {}",
                        rapidId, sofascoreId);
                PlayerDetailsResDto playerDetailsById = playerService.getPlayerDetailsById(rapidId, sofascoreId);
                return ResponseEntity.ok(playerDetailsById);
            }
        }catch (Exception e){
            logger.error("scores get player details error {} external id {}", e.getMessage(), sofascoreId);
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
