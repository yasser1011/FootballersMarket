package com.yasser.footballersmarket.user;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.yasser.footballersmarket.player.Player;
import com.yasser.footballersmarket.player.PlayerService;
import com.yasser.footballersmarket.player.dto.PlayerDetailsResDto;
import com.yasser.footballersmarket.playerstats.PlayerLeagueStats;
import com.yasser.footballersmarket.scores365.ScoresService;
import com.yasser.footballersmarket.scores365.dto.PlayerDetailsDto;
import com.yasser.footballersmarket.sofascore.SofascoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Service
public class UserService implements UserDetailsService {
    private final UserRepository userRepository;
    private final SofascoreService sofascoreService;
    private final ScoresService scoresService;
    private final PlayerService playerService;
    //private final AuthenticationManager authenticationManager;
    Logger logger = LoggerFactory.getLogger(UserService.class);

    public UserService(UserRepository userRepository, SofascoreService sofascoreService,
                       PlayerService playerService, ScoresService scoresService) {
        this.userRepository = userRepository;
        this.sofascoreService = sofascoreService;
        this.scoresService = scoresService;
        this.playerService = playerService;
      //  this.authenticationManager = authenticationManager;
    }

    public List<UserResponse> getAllUsersScores() throws JsonProcessingException {
        logger.info("getting all users scores");

        HashMap<Long, UserResponse> usersScoreMap = new HashMap<>();

        List<Object[]> allUsersScore = userRepository.getAllUsersScore();

        for (Object[] userPlayerRecord: allUsersScore){
            Long userId = ((BigInteger) userPlayerRecord[1]).longValue();
            String username = (String) userPlayerRecord[2];
            Integer userPoints = (Integer) userPlayerRecord[3];
            // case when user didn't buy any player
            if (userPlayerRecord[0] == null){
                usersScoreMap.put(userId, new UserResponse(userId, username, userPoints));
                continue;
            }

            Long playerId = ((BigInteger) userPlayerRecord[0]).longValue();
            Integer playerSofascoreId = (Integer) userPlayerRecord[4];
            String updatedBy = (String) userPlayerRecord[5];
            String playerSchedulerClub = (String) userPlayerRecord[7];
            String playerSofascoreCLub = (String) userPlayerRecord[6];
            Player pl = new Player(playerId, playerSchedulerClub, playerSofascoreCLub);
            PlayerDetailsResDto playerDetailsResDto = playerService.convertPlayerEntityToPlayerDto(pl);
            Integer playerPrice;
            if(!playerDetailsResDto.getAreClubStatsUpdated() || (updatedBy != null && updatedBy.contains("sofascore"))){
                PlayerDetailsDto playerDetailsDto = scoresService.fetchPlayerEntityFromExternalService(playerSofascoreId);
                PlayerLeagueStats leagueStatsDto = scoresService.createLeagueStatsDto(playerDetailsDto.getLeagueStats());
                playerService.updatePlayerLeagueStatsDetails(playerId, leagueStatsDto);
                PlayerDetailsResDto playerDetailsResDto1 = scoresService.convertDtoToPlayerDetailsDto(playerSofascoreId, playerDetailsDto, playerDetailsDto.getRecentMatches());
                playerPrice = playerDetailsResDto1.getPrice();
            }else{
                playerPrice = ((BigDecimal) userPlayerRecord[8]).intValue();
            }

            UserResponse userMapRecord = usersScoreMap.get(userId);
            if (userMapRecord == null){
                usersScoreMap.put(userId, new UserResponse(userId, username, userPoints + playerPrice));
            }else{
                Integer oldPoints = userMapRecord.getPoints();
                usersScoreMap.remove(userId);
                usersScoreMap.put(userId, new UserResponse(userId, username, oldPoints + playerPrice));
            }
        }
        ArrayList<UserResponse> userResponses = new ArrayList<>(usersScoreMap.values());
        return userResponses;
    }

    // return User instead of UserDetails for the userId value
    @Override
    public User loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findUserByUsername(username);
    }



    public UserResponse getUserByToken(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication.getPrincipal();
        if(principal instanceof String && principal.equals("anonymousUser"))
            return null;
        User user = (User)principal;
        return new UserResponse(user.getId(), user.getUsername(), user.getPoints());
    }


    @Transactional
    public void updateUserPoints(Long userId, Integer newPoints){
        userRepository.updateUserPoints(userId, newPoints);
    }

    public List<UserResponse> getTopRankedUsers(){
        List<Object[]> topRankedUsersObj = userRepository.getTopRankedUsers();

        List<UserResponse> topUsersResList = new ArrayList<>();

        for (Object[] userObj: topRankedUsersObj){
            Long userId = ((BigInteger) userObj[0]).longValue();
            String username = (String) userObj[1];
            Integer userPoints = ((BigDecimal) userObj[2]).intValue();

            topUsersResList.add(new UserResponse(userId, username, userPoints));
        }

        return topUsersResList;
    }

    public void saveUsersList(List<User> users){
        userRepository.saveAll(users);
    }

    public User findUserById(Long userId){
        return userRepository.findById(userId).orElseThrow(() ->
                new EntityNotFoundException("Player not found with id " + userId));
    }

    public void deleteAll(){
        userRepository.deleteAll();
    }

}
