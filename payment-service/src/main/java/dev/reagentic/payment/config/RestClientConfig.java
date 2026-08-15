package dev.reagentic.payment.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient accountClient(@Value("${account.service.url}") String accountServiceUrl) {
        return RestClient.builder().baseUrl(accountServiceUrl).build();
    }
}
