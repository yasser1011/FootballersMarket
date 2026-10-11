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



}
