package com.yasser.footballersmarket.playerusercurrentbought;

import com.yasser.footballersmarket.playerstats.PlayerLeagueStatsService;
import com.yasser.footballersmarket.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class PlayerUserCurrentBoughtController {
    private final PlayerUserCurrentBoughtService playerUserCurrentBoughtService;
    Logger logger = LoggerFactory.getLogger(PlayerUserCurrentBoughtController.class);

    public PlayerUserCurrentBoughtController(PlayerUserCurrentBoughtService playerUserCurrentBoughtService) {
        this.playerUserCurrentBoughtService = playerUserCurrentBoughtService;
    }

    @GetMapping("/player-transaction-type")
    public PlayerUserCurrentBoughtResponse checkIfPlayerCurrentlyBought(
            @RequestParam(name = "playerId") Long playerId){
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = user.getId();

//        Long userId = 1L;

        logger.info("fetching player transaction type, user id {} player id {}", userId, playerId);
        return playerUserCurrentBoughtService.
                isPlayerCurrentlyBoughtByUser(userId, playerId);

    }

    @GetMapping("/myteam")
    public List<PlayerUserCurrentBoughtDto> getUserTeam() throws InterruptedException {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = user.getId();

        logger.info("fetching user players team, user id {}", userId);
        return playerUserCurrentBoughtService.getUserBoughtPlayers(userId);
    }
}
