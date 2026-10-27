package com.yasser.footballersmarket.resttemplate;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class RestTemplateConfig {

    // @Primary keeps the consumers that inject RestTemplate by type resolving to this one.
    // generous rather than tight: these callers (player details, search, sofascore, rapid) have no
    // stale data to fall back on, and the 365scores athlete endpoint was measured at 18s cold
    // against a ~100ms warm path. the goal here is only that nothing hangs forever.
    @Primary
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(40))
                .build();
    }

    // featured players only. timeouts belong to a RestTemplate's request factory, not to a single
    // call, so bounding that one fetch takes its own instance. 365scores answers it in ~200ms warm
    // but was seen taking 61s cold, which held the request thread and the user's spinner open.
    @Bean("shortTimeoutRestTemplate")
    public RestTemplate shortTimeoutRestTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(3))
                .setReadTimeout(Duration.ofSeconds(5))
                .build();
    }
}
