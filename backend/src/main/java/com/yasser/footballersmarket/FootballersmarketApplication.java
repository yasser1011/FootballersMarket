package com.yasser.footballersmarket;

import com.yasser.footballersmarket.player.*;
import com.yasser.footballersmarket.playerstats.PlayerChampionsLeagueStatsRepository;
import com.yasser.footballersmarket.playerstats.PlayerLeagueStatsRepository;
import com.yasser.footballersmarket.playerstats.PlayerLeagueStatsService;
import com.yasser.footballersmarket.rapidapi.PlayersDataFetchingScheduler;
import com.yasser.footballersmarket.rapidapi.RapidApiConfig;
import com.yasser.footballersmarket.sofascore.SofascoreService;
import com.yasser.footballersmarket.transaction.TransactionRepository;
import com.yasser.footballersmarket.transaction.TransactionService;
import com.yasser.footballersmarket.user.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;

@SpringBootApplication
@EnableScheduling
@EnableTransactionManagement
@EnableAsync
public class FootballersmarketApplication {

	public static void main(String[] args) {
		SpringApplication.run(FootballersmarketApplication.class, args);
	}

	@GetMapping
	public String home(){
		return "home";
	}

	@Bean
	public CorsFilter corsFilter() {
		CorsConfiguration corsConfiguration = new CorsConfiguration();
		corsConfiguration.setAllowCredentials(true);
//		corsConfiguration.setAllowedOrigins(Arrays.asList("http://localhost:3000"));
		corsConfiguration.setAllowedOriginPatterns(Arrays.asList("*"));
		corsConfiguration.setAllowedHeaders(Arrays.asList("Origin", "Access-Control-Allow-Origin", "Content-Type",
				"Accept", "Authorization", "Origin, Accept", "X-Requested-With",
				"Access-Control-Request-Method", "Access-Control-Request-Headers"));
		corsConfiguration.setExposedHeaders(Arrays.asList("Origin", "Content-Type", "Accept", "Authorization",
				"Access-Control-Allow-Origin", "Access-Control-Allow-Origin", "Access-Control-Allow-Credentials"));
		corsConfiguration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
		UrlBasedCorsConfigurationSource urlBasedCorsConfigurationSource = new UrlBasedCorsConfigurationSource();
		urlBasedCorsConfigurationSource.registerCorsConfiguration("/**", corsConfiguration);
		return new CorsFilter(urlBasedCorsConfigurationSource);
	}

//	@Bean
//	CommandLineRunner commandLineRunner(PlayerRepository playerRepository,
//										RapidApiConfig rapidConfig,
//										PlayersDataFetchingScheduler playersDataScheduler,
//										PlayerLeagueStatsRepository leagueStatsRepository,
//										PlayerChampionsLeagueStatsRepository championsLeagueStatsRepository,
//										WebClient webClient,
//										SofascoreService sofascoreService,
//										PlayerService playerService,
//										UserRepository userRepository,
//										TransactionRepository transactionRepository,
//										TransactionService transactionService,
//										PasswordEncoder passwordEncoder,
//										PlayerLeagueStatsService playerLeagueStatsService){
//		return args -> {
////			User user1 = new User("user1", passwordEncoder.encode("pass1"), 1000);
////			User user2 = new User("user2", passwordEncoder.encode("pass2"), 1000);
////			User user3 = new User("user3", passwordEncoder.encode("pass2"), 1000);
////			User savedUser = userRepository.save(user1);
////			User savedUser2 = userRepository.save(user2);
////			User savedUser3 = userRepository.save(user3);
//// 			new runner
////			List<HashMap<String, String>> schedulerTeamsSearchInput = rapidConfig.getSchedulerTeamsSearchInput();
////			for (HashMap<String, String> searchTeam: schedulerTeamsSearchInput){
////				int totalNumPages = 1;
////				for (int i = 1; i <= totalNumPages; i++){
////					Integer totalNumOfPagesRes = playersDataScheduler.getPlayersData(String.valueOf(i),
////							searchTeam);
////					if(totalNumOfPagesRes != null)
////						totalNumPages = totalNumOfPagesRes;
////				}
////			}
////			playersDataScheduler.savePlayersMap();
//			// ------------------------------------
//////			// main runner
////			HashMap<String, String> searchInputParams = new HashMap<>();
////			searchInputParams.put("league", "140");
////			searchInputParams.put("season", "2023");
////			searchInputParams.put("team", "529"); // barca
////			int totalNumPages = 1;
////			for (int i = 1; i <= totalNumPages; i++){
////				Integer totalNumOfPagesRes = playersDataScheduler.getPlayersData(String.valueOf(i),
////						searchInputParams);
////				if(totalNumOfPagesRes != null)
////					totalNumPages = totalNumOfPagesRes;
////				System.out.println("total number of pages " + totalNumOfPagesRes);
////			}
////			searchInputParams.clear();
////			searchInputParams.put("league", "39");
////			searchInputParams.put("season", "2023");
////			searchInputParams.put("team", "50"); // man city
////			for (int i = 1; i <= totalNumPages; i++){
////				Integer totalNumOfPagesRes = playersDataScheduler.getPlayersData(String.valueOf(i), searchInputParams);
////				if (totalNumOfPagesRes == null) break;
////				totalNumPages = totalNumOfPagesRes;
////			}
////			playersDataScheduler.savePlayersMap();
//
////			// Pena
////			PlayerDetailsResDto player = playerService.getPlayerDetailsById(126L, 0);
////			UsernamePasswordAuthenticationToken authToken =
////					new UsernamePasswordAuthenticationToken(
////							savedUser,
////							null,
////							null
////					);
////			UsernamePasswordAuthenticationToken authToken2 =
////					new UsernamePasswordAuthenticationToken(
////							savedUser2,
////							null,
////							null
////					);
////			authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(new MockHttpServletRequest()));
////			authToken2.setDetails(new WebAuthenticationDetailsSource().buildDetails(new MockHttpServletRequest()));
////			SecurityContextHolder.getContext().setAuthentication(authToken);
////			transactionService.makePlayerTransaction(savedUser.getId(), savedUser.getPoints(), player.getId(),
////					player.getExternalServicePlayerId(), 1, player.getPrice());
////			SecurityContextHolder.getContext().setAuthentication(authToken2);
////			transactionService.makePlayerTransaction(savedUser2.getId(), savedUser2.getPoints(), player.getId(),
////					player.getExternalServicePlayerId(), 1, player.getPrice());
//
//////			---------------------------
//
//		};
//	}


}
