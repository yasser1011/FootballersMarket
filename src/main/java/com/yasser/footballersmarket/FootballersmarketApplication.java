package com.yasser.footballersmarket;

import com.yasser.footballersmarket.footballer.Footballer;
import com.yasser.footballersmarket.footballer.FootballerRepository;
import com.yasser.footballersmarket.footballer.Rating;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.List;

@SpringBootApplication
@RestController
public class FootballersmarketApplication {

	public static void main(String[] args) {
		SpringApplication.run(FootballersmarketApplication.class, args);
	}

	@GetMapping
	public String home(){
		return "home";
	}

	@Bean
	CommandLineRunner commandLineRunner(FootballerRepository footballerRepository){
		return args -> {
			Rating playerRating = new Rating(6.4, new Date());
			Rating playerRating2 = new Rating(7.5, new Date());
			Footballer player = new Footballer(242L,"id1", "france", 23, "left", "barca", false, 2, 3, List.of(playerRating, playerRating2));
			footballerRepository.save(player);
		};
	}

}
