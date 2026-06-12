package com.yasser.footballersmarket.player;

import com.yasser.footballersmarket.player.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

import javax.persistence.EntityNotFoundException;
import java.util.HashMap;
import java.util.List;

@RestController
@RequestMapping("/api/players")
public class PlayerController {
    private final WebClient webClient;
    private final PlayerService playerService;
    Logger logger = LoggerFactory.getLogger(PlayerController.class);

    public PlayerController(WebClient webClient,
                            PlayerService playerService) {
        this.webClient = webClient;
        this.playerService = playerService;
    }

    // home page players with pagination
    @GetMapping()
    public ResponseEntity getHomePagePlayers(@RequestParam(name = "page", defaultValue = "0") Integer page,
                                             @RequestParam(name = "worldCup", defaultValue = "false") boolean worldCup,
                                             @RequestParam(name = "teamId", required = false) Long teamId){
        try {
            // teamId is ignored unless worldCup is true
            Page<PlayerDetailsResDto> homePagePlayerData = playerService.getHomePagePlayerData(page, worldCup, teamId);
            return ResponseEntity.ok(homePagePlayerData);
        }catch (Exception e){
            logger.error("home page table error {}", e.getMessage());
            return ResponseEntity.internalServerError().body("error occurred");
        }
    }


    // get player details from database by player id
    @GetMapping("/{id}")
    public ResponseEntity getPlayerInfo(@PathVariable(value = "id") Long playerId){
        try {
            logger.info("get player details: player id {}", playerId);
//            PlayerDetailsResDto playerDbDetails = playerService.getPlayerDetailsById(playerId, 0);
            PlayerDetailsResDto playerDbDetails = playerService.getPlayerDetailsByInternalId(playerId);
            return ResponseEntity.ok(playerDbDetails);
        }catch (EntityNotFoundException e){
            logger.error("player not found player id {}", e.getMessage());
            return ResponseEntity.badRequest().body("player not found");
        }catch (Exception e){
            logger.error("get player details error {}", e.getMessage());
            return ResponseEntity.internalServerError().body("error occurred");
        }
    }

    // this is for search functionality to check if player exists in db with sofascore id
    @GetMapping("/sofascore")
    public ResponseEntity getPlayerBySofascoreId(@RequestParam(name = "sofascoreId") Integer sofascoreId) {
        try {
            logger.info("checking if player is in database by sofascore id {}", sofascoreId);
            HashMap<String, Object> response = new HashMap<>();
//            PlayerDetailsResDto playerDetailsBySofascoreId = playerService.getPlayerDetailsById(0L, sofascoreId);
            PlayerDetailsResDto playerDetailsBySofascoreId = playerService.getPlayerDetailsByExternalId(sofascoreId);
            response.put("player", playerDetailsBySofascoreId);
            if (playerDetailsBySofascoreId == null){
                logger.info("player not found in database {}", sofascoreId);
                response.put("status", -1);
            }else{
                logger.info("player found in database {}", sofascoreId);
                response.put("status", 0);
            }
            return ResponseEntity.ok(response);
        }catch (Exception e){
            logger.error("error in checking player {}", sofascoreId);
            return ResponseEntity.internalServerError().body("error occurred");
        }
    }


}
