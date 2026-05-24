package com.yasser.footballersmarket.featuredplayer;

import com.yasser.footballersmarket.scores365.dto.FeaturedPlayersResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/featured-players")
public class FeaturedPlayerController {

    private final FeaturedPlayerService featuredPlayerService;
    Logger logger = LoggerFactory.getLogger(FeaturedPlayerController.class);

    public FeaturedPlayerController(FeaturedPlayerService featuredPlayerService){
        this.featuredPlayerService = featuredPlayerService;
    }

    @GetMapping
    public ResponseEntity getFeaturedPlayerService() {
        try{
            FeaturedPlayersResponse featuredPlayersResponse = featuredPlayerService.getFeaturedPlayers();
            return ResponseEntity.ok(featuredPlayersResponse);
        }catch (Exception e){
            logger.error("featured player error {}", e.getMessage());
            return ResponseEntity.internalServerError().body("error occurred");
        }
    }
}
