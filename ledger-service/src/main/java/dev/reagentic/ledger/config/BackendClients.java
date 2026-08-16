package dev.reagentic.ledger.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class BackendClients {

    @Bean
    public RestClient accountClient(@Value("${ACCOUNT_SERVICE_URL:http://localhost:8082}") String url) {
        return RestClient.builder().baseUrl(url).build();
    }
}