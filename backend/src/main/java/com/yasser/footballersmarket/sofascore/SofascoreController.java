package com.yasser.footballersmarket.sofascore;

import com.yasser.footballersmarket.player.Player;
import com.yasser.footballersmarket.player.PlayerService;
import com.yasser.footballersmarket.player.dto.PlayerDetailsResDto;
import com.yasser.footballersmarket.scores365.dto.FeaturedPlayersResponse;
import com.yasser.footballersmarket.sofascore.dto.FeaturedMatch;
import com.yasser.footballersmarket.sofascore.dto.SearchPlayerEntity;
import com.yasser.footballersmarket.sofascore.dto.TrendingPlayersResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/old/api/ss")
public class SofascoreController {
    private final SofascoreService sofascoreService;
    private final PlayerService playerService;

    Logger logger = LoggerFactory.getLogger(SofascoreController.class);

    public SofascoreController(SofascoreService sofascoreService, PlayerService playerService) {
        this.sofascoreService = sofascoreService;
        this.playerService = playerService;
    }

    @GetMapping("/featured-match")
    public ResponseEntity getFeaturedEventMatch(){
        try{
            FeaturedMatch footballFeaturedMatch = sofascoreService.getFootballFeaturedMatch();
            return ResponseEntity.ok(footballFeaturedMatch);
        }catch (Exception e){
            logger.error("sofascore featured match error {}", e.getMessage());
            return ResponseEntity.internalServerError().body("error occurred");
        }

    }

    @GetMapping("/trending-players")
    public ResponseEntity getTrendingPlayers(){
        try{
            FeaturedPlayersResponse trendingPlayersResponse = sofascoreService.getTrendingPlayers();
            return ResponseEntity.ok(trendingPlayersResponse);
        }catch (Exception e){
            logger.error("sofascore trending players error {}", e.getMessage());
            return ResponseEntity.internalServerError().body("error occurred");
        }

    }
    @GetMapping("/players")
    public ResponseEntity getPlayerDetails(@RequestParam(name = "sofascoreId") Integer sofascoreId,
                                           @RequestParam(name = "rapidId", required = false) Long rapidId){
        try{
            if(rapidId == null){
                logger.info("getting player details from sofascore, sofascore id {}", sofascoreId);
                Player playerEntityFromSofascore = sofascoreService.getPlayerEntityFromSofascore(sofascoreId);
                return ResponseEntity.ok(playerEntityFromSofascore);
            }else{
                logger.info("getting player details from sofascore, player id {} sofascore id {}",
                        rapidId, sofascoreId);
                PlayerDetailsResDto playerDetailsById = playerService.getPlayerDetailsById(rapidId, sofascoreId);
                return ResponseEntity.ok(playerDetailsById);
            }
        }catch (Exception e){
            logger.error("sofascore get player details error {}", e.getMessage());
            return ResponseEntity.internalServerError().body("error occurred");
        }

    }

    @GetMapping("/players/search")
    public ResponseEntity getPlayerDetails(@RequestParam(name = "playerName") String playerName){
        try{
            logger.info("searching player name {}", playerName);
            List<SearchPlayerEntity> searchPlayerEntities = sofascoreService.searchPlayersByName(playerName);
            return ResponseEntity.ok(searchPlayerEntities);
        }catch (Exception e){
            logger.error("sofascore search player error {}", e.getMessage());
            return ResponseEntity.internalServerError().body("error occurred");
        }

    }

    @GetMapping("/players/recent-ratings/{sofascoreId}")
    public ResponseEntity getPlayerRecentRatings(@PathVariable(name = "sofascoreId") Integer sofascoreId){
        try{
            return ResponseEntity.ok(sofascoreService.getPlayerRecentRatings(sofascoreId));
        }catch (Exception e){
            logger.error("sofascore player recent ratings error {}", e.getMessage());
            return ResponseEntity.internalServerError().body("error occurred");
        }

    }
}
