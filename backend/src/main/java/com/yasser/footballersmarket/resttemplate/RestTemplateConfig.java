package com.yasser.footballersmarket.resttemplate;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class RestTemplateConfig {

    // @Primary keeps the consumers that inject RestTemplate by type resolving to this one
    @Primary
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
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
